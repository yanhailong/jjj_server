package com.jjg.game.alliance.handler;

import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.pb.req.*;
import com.jjg.game.alliance.service.AllianceAssetService;
import com.jjg.game.alliance.service.AllianceBattleService;
import com.jjg.game.alliance.service.AllianceDonateService;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.alliance.service.AllianceHelpService;
import com.jjg.game.alliance.service.AllianceRankService;
import com.jjg.game.alliance.service.AllianceService;
import com.jjg.game.alliance.service.AllianceShopService;
import com.jjg.game.alliance.service.AllianceTaskService;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * 联盟消息处理器。
 * <p>
 * 线程模型与 {@code SocialMessageHandler} 一致: 只读重 IO 接口(列表/排行/信息查询)
 * 经虚拟线程执行后直接回包, 不占用玩家绑定的 Disruptor worker;
 * 写路径(创建/加入/捐献/购买等)留在 worker 串行执行, 保证同一玩家操作顺序。
 *
 * @author 11
 * @date 2026/6/11
 */
@Component
@MessageType(MessageConst.MessageTypeDef.ALLIANCE)
public class AllianceMessageHandler implements GmListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //只读请求执行器: 虚拟线程按任务创建, 实际并发由 Mongo/Redis 连接池约束
    private final ExecutorService readExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("alliance-read-", 0).factory());

    @Autowired
    private AllianceService allianceService;
    @Autowired
    private AllianceTaskService taskService;
    @Autowired
    private AllianceHelpService helpService;
    @Autowired
    private AllianceShopService shopService;
    @Autowired
    private AllianceDonateService donateService;
    @Autowired
    private AllianceRankService rankService;
    @Autowired
    private AllianceBattleService battleService;
    @Autowired
    private AllianceEventService eventService;
    @Autowired
    private AllianceAssetService assetService;

    @PreDestroy
    public void shutdownReadExecutor() {
        readExecutor.shutdown();
    }

    /**
     * 只读请求转虚拟线程执行后回包
     */
    private void sendAsync(PlayerController pc, Supplier<Object> task) {
        readExecutor.execute(() -> {
            try {
                pc.send(task.get());
            } catch (Exception e) {
                log.error("联盟只读请求异步处理失败 playerId={}", pc.playerId(), e);
            }
        });
    }

    // --------------------------- 信息查询 (读路径) ---------------------------

    /**
     * 联盟主界面信息
     */
    @Command(AllianceConst.MsgBean.REQ_ALLIANCE_INFO)
    public void reqAllianceInfo(PlayerController pc, ReqAllianceInfo req) {
        sendAsync(pc, () -> allianceService.allianceInfo(pc.playerId()));
    }

    /**
     * 可加入联盟列表
     */
    @Command(AllianceConst.MsgBean.REQ_ALLIANCE_LIST)
    public void reqAllianceList(PlayerController pc, ReqAllianceList req) {
        sendAsync(pc, () -> allianceService.allianceList(pc.playerId()));
    }

    /**
     * 按 ID 搜索联盟
     */
    @Command(AllianceConst.MsgBean.REQ_SEARCH_ALLIANCE)
    public void reqSearchAlliance(PlayerController pc, ReqSearchAlliance req) {
        sendAsync(pc, () -> allianceService.search(pc.playerId(), req.allianceId));
    }

    /**
     * 成员列表
     */
    @Command(AllianceConst.MsgBean.REQ_MEMBER_LIST)
    public void reqMemberList(PlayerController pc, ReqMemberList req) {
        sendAsync(pc, () -> allianceService.memberList(pc.playerId()));
    }

    /**
     * 入盟申请列表 (盟主)
     */
    @Command(AllianceConst.MsgBean.REQ_APPLICATION_LIST)
    public void reqApplicationList(PlayerController pc, ReqApplicationList req) {
        sendAsync(pc, () -> allianceService.applicationList(pc.playerId()));
    }

    // --------------------------- 核心操作 (写路径) ---------------------------

    /**
     * 创建联盟
     */
    @Command(AllianceConst.MsgBean.REQ_CREATE_ALLIANCE)
    public void reqCreateAlliance(PlayerController pc, ReqCreateAlliance req) {
        pc.send(allianceService.create(pc, req.name, req.icon, req.notice, req.joinMinCasinoLevel, req.joinNeedAudit));
    }

    /**
     * 加入联盟 (allianceId=0 一键申请)
     */
    @Command(AllianceConst.MsgBean.REQ_JOIN_ALLIANCE)
    public void reqJoinAlliance(PlayerController pc, ReqJoinAlliance req) {
        pc.send(allianceService.join(pc.getPlayer(), req.allianceId));
    }

    /**
     * 退出联盟
     */
    @Command(AllianceConst.MsgBean.REQ_QUIT_ALLIANCE)
    public void reqQuitAlliance(PlayerController pc, ReqQuitAlliance req) {
        pc.send(allianceService.quit(pc.playerId()));
    }

    /**
     * 解散联盟 (盟主)
     */
    @Command(AllianceConst.MsgBean.REQ_DISSOLVE_ALLIANCE)
    public void reqDissolveAlliance(PlayerController pc, ReqDissolveAlliance req) {
        pc.send(allianceService.dissolve(pc.playerId()));
    }

    /**
     * 编辑联盟 (盟主)
     */
    @Command(AllianceConst.MsgBean.REQ_EDIT_ALLIANCE)
    public void reqEditAlliance(PlayerController pc, ReqEditAlliance req) {
        pc.send(allianceService.edit(pc.playerId(), req.name, req.icon, req.notice,
                req.joinMinCasinoLevel, req.joinNeedAudit));
    }

    /**
     * 踢出成员 (盟主)
     */
    @Command(AllianceConst.MsgBean.REQ_KICK_MEMBER)
    public void reqKickMember(PlayerController pc, ReqKickMember req) {
        pc.send(allianceService.kick(pc.playerId(), req.playerId));
    }

    /**
     * 转让盟主
     */
    @Command(AllianceConst.MsgBean.REQ_TRANSFER_LEADER)
    public void reqTransferLeader(PlayerController pc, ReqTransferLeader req) {
        pc.send(allianceService.transferLeader(pc.playerId(), req.playerId));
    }

    /**
     * 处理入盟申请 (盟主, 支持一键)
     */
    @Command(AllianceConst.MsgBean.REQ_HANDLE_APPLICATION)
    public void reqHandleApplication(PlayerController pc, ReqHandleApplication req) {
        pc.send(allianceService.handleApplications(pc.getPlayer(), req.playerIds, req.agree));
    }

    // --------------------------- 联盟任务 ---------------------------

    /**
     * 任务列表 (含惰性补齐, 有写可能, 留在 worker)
     */
    @Command(AllianceConst.MsgBean.REQ_TASK_LIST)
    public void reqTaskList(PlayerController pc, ReqAllianceTaskList req) {
        pc.send(taskService.taskList(pc.playerId()));
    }

    /**
     * 接取任务
     */
    @Command(AllianceConst.MsgBean.REQ_ACCEPT_TASK)
    public void reqAcceptTask(PlayerController pc, ReqAllianceAcceptTask req) {
        pc.send(taskService.acceptTask(pc.playerId(), req.taskCfgId));
    }

    /**
     * 放弃任务
     */
    @Command(AllianceConst.MsgBean.REQ_ABANDON_TASK)
    public void reqAbandonTask(PlayerController pc, ReqAbandonTask req) {
        pc.send(taskService.abandonTask(pc.playerId()));
    }

    /**
     * 已完成任务列表 (最近N条, 只读)
     */
    @Command(AllianceConst.MsgBean.REQ_FINISHED_TASK)
    public void reqFinishedTask(PlayerController pc, ReqAllianceFinishedTask req) {
        sendAsync(pc, () -> taskService.finishedTaskList(pc.playerId()));
    }

    /**
     * 刷新任务 (扣道具 + 改任务池的写路径, 留在 worker 串行)
     */
    @Command(AllianceConst.MsgBean.REQ_REFRESH_TASK)
    public void reqAllianceRefreshTask(PlayerController pc, ReqAllianceRefreshTask req) {
        pc.send(taskService.refreshTaskList(pc));
    }

    // --------------------------- 成员互助 ---------------------------

    /**
     * 发起求助
     */
    @Command(AllianceConst.MsgBean.REQ_SEEK_HELP)
    public void reqSeekHelp(PlayerController pc, ReqAllianceSeekHelp req) {
        pc.send(helpService.seekHelp(pc.playerId(), req.type, req.targetId, req.targetName));
    }

    /**
     * 帮助单个订单 (点击助力)
     */
    @Command(AllianceConst.MsgBean.REQ_HELP)
    public void reqHelp(PlayerController pc, ReqAllianceHelp req) {
        pc.send(helpService.help(pc.playerId(), req.orderId));
    }

    /**
     * 一键帮助 (仅建筑加速)
     */
    @Command(AllianceConst.MsgBean.REQ_ONE_KEY_HELP)
    public void reqOneKeyHelp(PlayerController pc, ReqOneKeyHelp req) {
        pc.send(helpService.oneKeyHelp(pc.playerId()));
    }

    /**
     * 求助订单列表 (含惰性清理, 留在 worker)
     */
    @Command(AllianceConst.MsgBean.REQ_HELP_LIST)
    public void reqHelpList(PlayerController pc, ReqAllianceHelpList req) {
        pc.send(helpService.helpList(pc.playerId()));
    }

    /**
     * 按 orderId 获取求助详情 (只读)
     */
    @Command(AllianceConst.MsgBean.REQ_GET_HELP_INFO)
    public void reqGetHelpInfo(PlayerController pc, ReqGetHelpInfo req) {
        sendAsync(pc, () -> helpService.getHelpInfo(pc.playerId(), req.orderId));
    }

    // --------------------------- 商店 / 捐献 ---------------------------

    /**
     * 商店列表
     */
    @Command(AllianceConst.MsgBean.REQ_SHOP_LIST)
    public void reqShopList(PlayerController pc, ReqAllianceShopList req) {
        sendAsync(pc, () -> shopService.shopList(pc.playerId()));
    }

    /**
     * 购买商品
     */
    @Command(AllianceConst.MsgBean.REQ_SHOP_BUY)
    public void reqShopBuy(PlayerController pc, ReqAllianceShopBuy req) {
        pc.send(shopService.buy(pc.playerId(), req.goodsId, req.count));
    }

    /**
     * 捐献界面信息
     */
    @Command(AllianceConst.MsgBean.REQ_DONATE_INFO)
    public void reqDonateInfo(PlayerController pc, ReqAllianceDonateInfo req) {
        sendAsync(pc, () -> donateService.donateInfo(pc.playerId()));
    }

    /**
     * 捐献
     */
    @Command(AllianceConst.MsgBean.REQ_DONATE)
    public void reqDonate(PlayerController pc, ReqAllianceDonate req) {
        pc.send(donateService.donate(pc));
    }

    // --------------------------- 排行榜 ---------------------------

    /**
     * 贡献度周榜 (盟内)
     */
    @Command(AllianceConst.MsgBean.REQ_CONTRIB_RANK)
    public void reqContribRank(PlayerController pc, ReqContribRank req) {
        sendAsync(pc, () -> rankService.contribRank(pc.playerId()));
    }

    /**
     * 联盟声誉排行 (全服)
     */
    @Command(AllianceConst.MsgBean.REQ_ALLIANCE_RANK)
    public void reqAllianceRank(PlayerController pc, ReqAllianceRank req) {
        sendAsync(pc, () -> rankService.allianceRank(pc.playerId()));
    }

    // --------------------------- 联盟对决 ---------------------------

    /**
     * 对决信息
     */
    @Command(AllianceConst.MsgBean.REQ_BATTLE_INFO)
    public void reqBattleInfo(PlayerController pc, ReqAllianceBattleInfo req) {
        sendAsync(pc, () -> battleService.battleInfo(pc.playerId()));
    }

    /**
     * 对决报名 (盟主)
     */
    @Command(AllianceConst.MsgBean.REQ_BATTLE_SIGNUP)
    public void reqBattleSignup(PlayerController pc, ReqAllianceBattleSignup req) {
        pc.send(battleService.signup(pc.playerId()));
    }

    /**
     * 对决贡献榜单
     */
    @Command(AllianceConst.MsgBean.REQ_BATTLE_RANK)
    public void reqBattleRank(PlayerController pc, ReqAllianceBattleRank req) {
        sendAsync(pc, () -> battleService.battleRank(pc.playerId()));
    }

    /**
     * 领取对决阶段奖励
     */
    @Command(AllianceConst.MsgBean.REQ_BATTLE_STAGE_CLAIM)
    public void reqBattleStageClaim(PlayerController pc, ReqBattleStageClaim req) {
        pc.send(battleService.claimStage(pc.playerId(), req.stage));
    }

    // --------------------------- GM (联调测试) ---------------------------

    @Override
    public CommonResult<String> gm(PlayerController pc, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            String cmd = gmOrders[0];
            if ("allianceInfo".equalsIgnoreCase(cmd)) {
                reqAllianceInfo(pc, null);
            } else if ("allianceCreate".equalsIgnoreCase(cmd)) {
                ReqCreateAlliance req = new ReqCreateAlliance();
                req.name = gmOrders.length > 1 ? gmOrders[1] : "测试联盟";
                req.joinMinCasinoLevel = 1;
                reqCreateAlliance(pc, req);
            } else if ("allianceJoin".equalsIgnoreCase(cmd)) {
                ReqJoinAlliance req = new ReqJoinAlliance();
                req.allianceId = gmOrders.length > 1 ? Long.parseLong(gmOrders[1]) : 0;
                reqJoinAlliance(pc, req);
            } else if ("allianceDonate".equalsIgnoreCase(cmd)) {
                ReqAllianceDonate req = new ReqAllianceDonate();
                reqDonate(pc, req);
            } else if ("allianceTasks".equalsIgnoreCase(cmd)) {
                reqTaskList(pc, null);
            } else if ("allianceEvent".equalsIgnoreCase(cmd)) {
                //模拟事件上报: allianceEvent <conditionId> <param> <value>
                eventService.onEvent(pc.playerId(), Integer.parseInt(gmOrders[1]),
                        Long.parseLong(gmOrders[2]), Long.parseLong(gmOrders[3]));
            } else if ("allianceBattleTick".equalsIgnoreCase(cmd)) {
                //联调: 立即触发一次对决状态机推进
                battleService.tick();
            } else if ("allianceBattle".equalsIgnoreCase(cmd)) {
                reqBattleInfo(pc, null);
            } else if ("addcontribut".equalsIgnoreCase(cmd)) {
                long contribution = Long.parseLong(gmOrders[1]);
                long allianceId = 0;
                if(gmOrders.length == 3){
                    allianceId = Long.parseLong(gmOrders[2]);
                }
                assetService.grantContribution(pc.playerId(), contribution, 0, allianceId);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
