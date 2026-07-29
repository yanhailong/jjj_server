package com.jjg.game.sim.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.logger.SimGuideLogger;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.req.*;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * 模拟经营游戏消息处理器
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SIM_GAME)
public class SimMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SimManager simManager;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Autowired
    private SimGuestService guestService;
    @Autowired
    private SimBuildingService buildingService;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private SimSkillService skillService;
    @Autowired
    private SimCasinoService casinoService;
    @Autowired
    private SimStatsService statsService;
    @Autowired
    private SimConfigCacheService configCacheService;
    @Autowired
    private SimTaskService taskService;
    @Autowired
    private SimMedalService medalService;
    @Autowired
    private SimVisitService visitService;
    @Autowired
    private SimCoopTaskService coopTaskService;
    @Autowired
    private SimCoopRoomRouteService coopRoomRouteService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimGuideLogger simGuideLogger;
    @Autowired
    private SimGuideService guideService;


    /**
     * 进入游戏
     */
    @Command(SimConstant.MsgBean.REQ_ENTER_GAME)
    public void reqEnterGame(PlayerController playerController, ReqSimEnterGame req) {
        simManager.onEnterGame(playerController);
    }

    /**
     * 退出
     */
    @Command(SimConstant.MsgBean.REQ_EXIT_GAME)
    public void reqExitGame(PlayerController playerController, ReqSimExitGame req) {
        //退出 sim 界面不卸载 ctx, 场景后台继续运行直到玩家下线
        playerController.setScene(null);
    }

    /**
     * 完成新手引导
     */
    @Command(SimConstant.MsgBean.REQ_FINISH_GUIDE)
    public void reqFinishGuide(PlayerController playerController, ReqFinishGuide req) {
        SimGuideService.FinishGuideResult result =
                simManager.onFinishGuideWithTriggers(playerController.playerId(), req.guideId);
        // 同一线程、同一玩家连接依次入发送队列，保证前端先收到完成响应，再收到新引导通知。
        playerController.send(result.response());
        simManager.notifyGuideTriggers(playerController.playerId(), result.triggeredGuideGroupIds());
    }

    //--------------------------Casino相关 begin--------------------------

    /**
     * 开辟新场景
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_CASINO)
    public void reqUnlockCasino(PlayerController playerController, ReqUnlockCasino req) {
        execute(playerController, ctx -> {
            casinoService.onUnlockCasino(ctx, req.casinoId);
        });
    }

    /**
     * 切换场景
     */
    @Command(SimConstant.MsgBean.REQ_SWITCH_CASINO)
    public void reqSwitchCasino(PlayerController playerController, ReqSwitchCasino req) {
        execute(playerController, ctx -> {
            casinoService.onSwitchCasino(ctx, req.casinoId);
        });
    }

    /**
     * 获取当前场景信息
     */
    @Command(SimConstant.MsgBean.REQ_CASINO_INFO)
    public void reqSimCasinoInfo(PlayerController playerController, ReqSimCasinoInfo req) {
        execute(playerController, ctx -> {
            //回到自己场景即结束拜访态, 之后查留言板看到的是自己的
            ctx.setVisitTargetId(0);
            casinoService.onCasinoInfo(ctx);
        });
    }

    //--------------------------建筑相关 begin--------------------------

    /**
     * 解锁建筑
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_BUILDING)
    public void reqUnlockBuilding(PlayerController playerController, ReqUnlockBuilding req) {
        execute(playerController, ctx -> {
            ResUnlockBuilding res = buildingService.onUnlockBuilding(ctx, req.id);
            ctx.send(res);
        });
    }

    /**
     * 获取建筑信息
     */
    @Command(SimConstant.MsgBean.REQ_BUILDING_INFO)
    public void reqBuildingInfo(PlayerController playerController, ReqBuildingInfo req) {
        execute(playerController, ctx -> {
            buildingService.onBuildingInfo(ctx, req.id);
        });
    }

    /**
     * 升级建筑 (启动 CD)
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_BUILDING)
    public void reqUpgradeBuilding(PlayerController playerController, ReqUpgradeBuilding req) {
        execute(playerController, ctx -> {
            buildingService.onUpgradeBuilding(ctx, req.id);
        });
    }

    /**
     * 完成建筑升级
     */
    @Command(SimConstant.MsgBean.REQ_COMPLETE_BUILDING_UPGRADE)
    public void reqCompleteBuildingUpgrade(PlayerController playerController, ReqCompleteBuildingUpgrade req) {
        execute(playerController, ctx -> {
            buildingService.onCompleteBuildingUpgrade(ctx, req.id);
        });
    }

    /**
     * 清除升级 CD
     */
    @Command(SimConstant.MsgBean.REQ_CLEAR_BUILDING_CD)
    public void reqClearBuildingCD(PlayerController playerController, ReqClearBuildingCD req) {
        execute(playerController, ctx -> {
            buildingService.onClearBuildingCD(ctx, req.id, req.costCount, req.watchAd);
        });
    }

    /**
     * 领取离线收益
     */
    @Command(SimConstant.MsgBean.REQ_CLAIM_OFFLINE_REWARD)
    public void reqClaimOfflineReward(PlayerController playerController, ReqClaimOfflineReward req) {
        execute(playerController, ctx -> {
            buildingService.onClaimOfflineReward(ctx, req.watchAd);
        });
    }


    //--------------------------雇员相关 begin--------------------------

    /**
     * 招募雇员
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_EMPLOYEE)
    public void reqRecruitEmployee(PlayerController playerController, ReqRecruitEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onRecruitEmployee(ctx, req.count);
        });
    }

    /**
     * 升级雇员
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_EMPLOYEE)
    public void reqUpgradeEmployee(PlayerController playerController, ReqUpgradeEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onUpgradeEmployee(ctx, req.employeeId);
        });
    }

    /**
     * 升星雇员
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_EMPLOYEE)
    public void reqStarUpEmployee(PlayerController playerController, ReqStarUpEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onStarUpEmployee(ctx, req.employeeId);
        });
    }

    /**
     * 任命/更换主管
     */
    @Command(SimConstant.MsgBean.REQ_ASSIGN_SUPERVISOR)
    public void reqAssignSupervisor(PlayerController playerController, ReqAssignSupervisor req) {
        execute(playerController, ctx -> {
            employeeService.onAssignSupervisor(ctx, req.employeeId);
        });
    }


    /**
     * 获取所有雇员
     */
    @Command(SimConstant.MsgBean.REQ_ALL_EMPLOYEE)
    public void reqAllEmployee(PlayerController playerController, ReqAllEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onAllEmployee(ctx);
        });
    }

    /**
     * 获取雇员卡池
     */
    @Command(SimConstant.MsgBean.REQ_EMPLOYEE_POOL)
    public void reqEmployeePool(PlayerController playerController, ReqEmployeePool req) {
        execute(playerController, ctx -> {
            employeeService.onPool(ctx);
        });
    }

    //--------------------------雇员相关 end--------------------------


    @Command(SimConstant.MsgBean.REQ_SIM_GET_SKILLS)
    public void reqSlotsGetSkills(PlayerController playerController, ReqSimGetSkills req) {
        execute(playerController, ctx -> {
            skillService.onLoadSlotsSkills(ctx, req.gameType);
        });
    }

    @Command(SimConstant.MsgBean.REQ_SIM_UPGRADE_SKILL)
    public void reqSimUpgradeSkill(PlayerController playerController, ReqSimUpgradeSkill req) {
        execute(playerController, ctx -> {
            skillService.onUpgradeSkill(ctx, req.gameType, req.skillId);
        });
    }

    //--------------------------游客相关 begin--------------------------

    /**
     * 生成购买游客
     */
    @Command(SimConstant.MsgBean.REQ_GEN_PURCHASED_GUEST)
    public void reqGenPurchasedGuest(PlayerController playerController, ReqGenPurchasedGuest req) {
        execute(playerController, ctx -> {
            guestService.generatePurchasedGuest(ctx, req.guestId, req.count);
        });
    }

    /**
     * 领取购买游客奖励 (凭 uid 结算奖励)
     */
    @Command(SimConstant.MsgBean.REQ_PURCHASED_GUEST_REWARD)
    public void reqPurchasedGuestReward(PlayerController playerController, ReqPurchasedGuestReward req) {
        execute(playerController, ctx -> {
            guestService.claimPurchasedGuestReward(ctx, req.uid, req.index);
        });
    }

    /**
     * 获取所有游客
     */
    @Command(SimConstant.MsgBean.REQ_ALL_GUEST)
    public void reqAllGuest(PlayerController playerController, ReqAllGuest req) {
        execute(playerController, ctx -> {
            guestService.onAllGuest(ctx);
        });
    }

    /**
     * 招募游客
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_GUEST)
    public void reqRecruitGuest(PlayerController playerController, ReqRecruitGuest req) {
        execute(playerController, ctx -> {
            guestService.onRecruitGuest(ctx, req.count);
        });
    }

    /**
     * 升星游客
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_GUEST)
    public void reqStarUpGuest(PlayerController playerController, ReqStarUpGuest req) {
        execute(playerController, ctx -> {
            guestService.onStarUpGuest(ctx, req.guestId);
        });
    }

    /**
     * 游客羁绊
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_BONDS)
    public void reqUnlockBonds(PlayerController playerController, ReqGuestBonds req) {
        execute(playerController, ctx -> {
            guestService.onBonds(ctx);
        });
    }

    /**
     * 获取游客卡池
     */
    @Command(SimConstant.MsgBean.REQ_GUEST_POOL)
    public void reqSimPool(PlayerController playerController, ReqGuestPool req) {
        execute(playerController, ctx -> {
            guestService.onPool(ctx);
        });
    }
    //--------------------------游客相关 end--------------------------

    //--------------------------经营信息 begin--------------------------

    /**
     * 经营信息-运营数据
     */
    @Command(SimConstant.MsgBean.REQ_OPERATION_DATA)
    public void reqOperationData(PlayerController playerController, ReqOperationData req) {
        execute(playerController, ctx -> {
            statsService.onOperationData(ctx);
        });
    }

    /**
     * 经营信息-SPINE游戏数据 (>0指定游戏, 0所有游戏汇总)
     */
    @Command(SimConstant.MsgBean.REQ_SLOT_STAT)
    public void reqSlotStat(PlayerController playerController, ReqSlotStat req) {
        execute(playerController, ctx -> {
            statsService.onSlotStat(ctx, req.gameType);
        });
    }

    //--------------------------经营信息 end--------------------------

    /**
     * 获取玩家信息
     */
    @Command(SimConstant.MsgBean.REQ_SIM_PLAYER_INFO)
    public void reqSimPlayerInfo(PlayerController playerController, ReqSimPlayerInfo req) {
        simManager.simPlayerInfo(playerController, req.playerId);
    }

    //--------------------------任务 (主线/成就) begin--------------------------

    /**
     * 获取任务列表 (主线 + 成就)
     */
    @Command(SimConstant.MsgBean.REQ_SIM_TASK_LIST)
    public void reqSimTaskList(PlayerController playerController, ReqSimTaskList req) {
        execute(playerController, ctx -> ctx.send(taskService.buildTaskList(ctx)));
    }

    /**
     * 领取任务奖励
     */
    @Command(SimConstant.MsgBean.REQ_SIM_TASK_REWARD)
    public void reqSimTaskReward(PlayerController playerController, ReqSimTaskReward req) {
        execute(playerController, ctx -> ctx.send(taskService.claimReward(ctx, req.taskId)));
    }

    /**
     * 设置经营信息中展示的成就勋章
     */
    @Command(SimConstant.MsgBean.REQ_SET_DISPLAYED_MEDALS)
    public void reqSetDisplayedMedals(PlayerController playerController, ReqSetDisplayedMedals req) {
        execute(playerController, ctx -> ctx.send(taskService.setDisplayedMedals(ctx, req.medalIds)));
    }

    /**
     * 成就勋章面板 (达成统计/全服排行/品质统计/加成档)
     */
    @Command(SimConstant.MsgBean.REQ_MEDAL_PANEL)
    public void reqMedalPanel(PlayerController playerController, ReqMedalPanel req) {
        execute(playerController, ctx -> ctx.send(medalService.buildMedalPanel(ctx)));
    }

    /**
     * 修改展示中的勋章
     */
    @Command(SimConstant.MsgBean.REQ_CHANGE_SHOW_MEDAL)
    public void reqChangeShowMwdal(PlayerController playerController, ReqChangeShowMedal req) {
        execute(playerController, ctx -> ctx.send(medalService.changeShowMedal(ctx, req.newMedalId)));
    }

    //--------------------------任务 (主线/成就) end--------------------------

    //--------------------------多人协作任务 begin--------------------------

    /**
     * 多人任务今日列表
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_LIST)
    public void reqCoopTaskList(PlayerController playerController, ReqCoopTaskList req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.buildTaskList(ctx)));
    }

    /**
     * 刷新多人任务列表 (每日首免, 之后耗道具)
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_REFRESH)
    public void reqCoopTaskRefresh(PlayerController playerController, ReqCoopTaskRefresh req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.refresh(ctx)));
    }

    /**
     * 领取多人任务
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_CLAIM)
    public void reqCoopTaskClaim(PlayerController playerController, ReqCoopTaskClaim req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.claim(ctx, req.taskId)));
    }

    /**
     * 发起者领取多人任务奖励
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_REWARD)
    public void reqCoopTaskReward(PlayerController playerController, ReqCoopTaskReward req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.claimReward(ctx, req.taskId)));
    }

    /**
     * 创建协作房间 (成功后切到 slots 节点, 服务内已先回包)
     */
    @Command(SimConstant.MsgBean.REQ_CREATE_COOP_ROOM)
    public void reqCreateCoopRoom(PlayerController playerController, ReqCreateCoopRoom req) {
        execute(playerController, ctx -> {
            ResCreateCoopRoom res = coopRoomRouteService.createRoom(ctx, req.taskId, req.gameType, req.roomCfgId);
            //成功路径服务内已回包并切节点; 失败时这里回包
            if (res != null) {
                ctx.send(res);
            }
        });
    }

    /**
     * 加入协作房间 (成功后切到房间所在 slots 节点, 服务内已先回包)
     */
    @Command(SimConstant.MsgBean.REQ_JOIN_COOP_ROOM)
    public void reqJoinCoopRoom(PlayerController playerController, ReqJoinCoopRoom req) {
        execute(playerController, ctx -> {
            ResJoinCoopRoom res = coopRoomRouteService.joinRoom(ctx, req.roomId);
            if (res != null) {
                ctx.send(res);
            }
        });
    }

    /**
     * 获取战力
     */
    @Command(SimConstant.MsgBean.REQ_COMBAT_POWER)
    public void reqCombatPower(PlayerController playerController, ReqCombatPower req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.combatPowers(ctx)));
    }

    //--------------------------多人协作任务 end--------------------------

    //--------------------------拜访相关 begin--------------------------

    @Command(SimConstant.MsgBean.REQ_VISIT_CASINO)
    public void reqVisitCasino(PlayerController playerController, ReqVisitCasino req) {
        executeVisit(playerController, ctx -> visitService.visit(ctx, req.playerId, req.casinoId),
                ResVisitCasino::new);
    }

    @Command(SimConstant.MsgBean.REQ_RANDOM_VISIT)
    public void reqRandomVisit(PlayerController playerController, ReqRandomVisit req) {
        executeVisit(playerController, ctx -> visitService.randomVisit(ctx, req.lastPlayerId),
                ResVisitCasino::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_LIKE)
    public void reqVisitLike(PlayerController playerController, ReqVisitLike req) {
        executeVisit(playerController, ctx -> visitService.like(ctx, req.playerId, req.casinoId),
                ResVisitAction::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_COMMENT)
    public void reqVisitComment(PlayerController playerController, ReqVisitComment req) {
        executeVisit(playerController,
                ctx -> visitService.comment(ctx, req.playerId, req.casinoId, req.content),
                ResVisitAction::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_GIFT)
    public void reqVisitGift(PlayerController playerController, ReqVisitGift req) {
        executeVisit(playerController,
                ctx -> visitService.gift(ctx, req.playerId, req.casinoId, req.giftId),
                ResVisitAction::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_RECORDS)
    public void reqVisitRecords(PlayerController playerController, ReqVisitRecords req) {
        sendVisit(playerController,
                () -> visitService.records(playerController.playerId(), req.offset, req.limit),
                ResVisitRecords::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_COMMENTS)
    public void reqVisitComments(PlayerController playerController, ReqVisitComments req) {
        executeVisit(playerController, ctx -> visitService.comments(ctx, req.offset, req.limit),
                ResVisitComments::new);
    }

    @Command(SimConstant.MsgBean.REQ_DELETE_VISIT_COMMENT)
    public void reqDeleteVisitComment(PlayerController playerController, ReqDeleteVisitComment req) {
        sendVisit(playerController,
                () -> visitService.deleteComment(playerController.playerId(), req.commentId),
                ResDeleteVisitComment::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_SUMMARY)
    public void reqVisitSummary(PlayerController playerController, ReqVisitSummary req) {
        sendVisit(playerController, () -> visitService.summary(playerController.playerId()),
                ResVisitSummary::new);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_RANK)
    public void reqVisitRank(PlayerController playerController, ReqVisitRank req) {
        sendVisit(playerController, () -> visitService.rank(playerController.playerId()),
                ResVisitRank::new);
    }

    @Command(SimConstant.MsgBean.REQ_ENTER_VISIT_GAME)
    public void reqEnterVisitGame(PlayerController playerController, ReqEnterVisitGame req) {
        execute(playerController, ctx -> {
            ResVisitTrial res = visitService.enterVisitGame(ctx, req.playerId, req.casinoId, req.gameType);
            //成功路径服务内已回包并切节点; 失败时这里回包
            if (res != null) {
                ctx.send(res);
            }
        });
    }

    @Command(SimConstant.MsgBean.REQ_EXIT_VISIT_TRIAL)
    public void reqExitVisitTrial(PlayerController playerController, ReqExitVisitTrial req) {
        sendVisit(playerController, () -> visitService.exitTrial(playerController.playerId()),
                ResVisitTrial::new);
    }

    //--------------------------拜访相关 end--------------------------


    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("simFinishGuide".equalsIgnoreCase(gmOrders[0])) {
                // 后台命令：simFinishGuide all 或 simFinishGuide 1001,1002,1003
                if (gmOrders.length < 2) {
                    res.code = Code.PARAM_ERROR;
                    res.data = "参数错误，格式：simFinishGuide all 或 simFinishGuide <GuideId,GuideId>";
                    return res;
                }
                long playerId = playerController.playerId();
                String param = gmOrders[1].trim();
                if ("all".equalsIgnoreCase(param)) {
                    res.code = simManager.onGmFinishAllGuides(playerId);
                    if (res.code == Code.SUCCESS) {
                        res.data = "已完成全部引导";
                        simGuideLogger.completed(playerId, SimGuideLogger.OPERATION_FINISH_ALL, null);
                    } else {
                        res.data = "玩家模拟经营数据未加载，无法完成全部引导";
                    }
                    return res;
                }

                LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>();
                for (String value : param.split(",", -1)) {
                    if (value == null || !value.trim().matches("\\d+")) {
                        res.code = Code.PARAM_ERROR;
                        res.data = "引导ID格式错误：" + value;
                        return res;
                    }
                    int guideId = Integer.parseInt(value.trim());
                    if (guideId <= 0) {
                        res.code = Code.PARAM_ERROR;
                        res.data = "引导ID必须大于0：" + guideId;
                        return res;
                    }
                    uniqueIds.add(guideId);
                }
                List<Integer> guideIds = new ArrayList<>(uniqueIds);
                res.code = simManager.onGmFinishGuides(playerId, guideIds);
                if (res.code == Code.SUCCESS) {
                    res.data = "已完成指定引导：" + guideIds;
                    simGuideLogger.completed(playerId,
                            SimGuideLogger.OPERATION_FINISH_SPECIFIED, guideIds);
                } else if (res.code == Code.PARAM_ERROR) {
                    res.data = "指定列表中存在未配置的引导ID：" + guideIds;
                } else {
                    res.data = "玩家模拟经营数据未加载，无法完成指定引导：" + guideIds;
                }
                return res;
            } else if ("printGuest".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = this.simPlayerContextRegistry.getContext(playerController.playerId());
                context.printGuest();
            } else if ("printBuilding".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext context = this.simPlayerContextRegistry.getContext(playerController.playerId());
                context.printBuilding();
            } else if ("unlockGuest".equalsIgnoreCase(gmOrders[0])) {
                int guestId = Integer.parseInt(gmOrders[1]);
                execute(playerController, ctx -> {
                    guestService.unlockGuest(ctx, guestId);
                });
            } else if ("unlockBuilding".equalsIgnoreCase(gmOrders[0])) {
                ReqUnlockBuilding req = new ReqUnlockBuilding();
                req.id = Integer.parseInt(gmOrders[1]);
                reqUnlockBuilding(playerController, req);
            } else if ("buildingInfo".equalsIgnoreCase(gmOrders[0])) {
                ReqBuildingInfo req = new ReqBuildingInfo();
                req.id = Integer.parseInt(gmOrders[1]);
                reqBuildingInfo(playerController, req);
            } else if ("upgradeEmployee".equalsIgnoreCase(gmOrders[0])) {
                ReqUpgradeEmployee req = new ReqUpgradeEmployee();
                req.employeeId = Integer.parseInt(gmOrders[1]);
                reqUpgradeEmployee(playerController, req);
            } else if ("starUpEmployee".equalsIgnoreCase(gmOrders[0])) {
                ReqStarUpEmployee req = new ReqStarUpEmployee();
                req.employeeId = Integer.parseInt(gmOrders[1]);
                reqStarUpEmployee(playerController, req);
            } else if ("assignSupervisor".equalsIgnoreCase(gmOrders[0])) {
                ReqAssignSupervisor req = new ReqAssignSupervisor();
                req.employeeId = Integer.parseInt(gmOrders[1]);
                reqAssignSupervisor(playerController, req);
            } else if ("recruitEmployee".equalsIgnoreCase(gmOrders[0])) {
                ReqRecruitEmployee req = new ReqRecruitEmployee();
                req.count = gmOrders.length > 1 ? Integer.parseInt(gmOrders[1]) : 1;
                reqRecruitEmployee(playerController, req);
            } else if ("claimOffline".equalsIgnoreCase(gmOrders[0])) {
                boolean watchAd = gmOrders.length > 1 && "1".equals(gmOrders[1]);
                ReqClaimOfflineReward req = new ReqClaimOfflineReward();
                req.watchAd = watchAd;
                reqClaimOfflineReward(playerController, req);
            } else if ("unlockCasino".equalsIgnoreCase(gmOrders[0])) {
                ReqUnlockCasino req = new ReqUnlockCasino();
                req.casinoId = Integer.parseInt(gmOrders[1]);
                reqUnlockCasino(playerController, req);
            } else if ("switchCasino".equalsIgnoreCase(gmOrders[0])) {
                ReqSwitchCasino req = new ReqSwitchCasino();
                req.casinoId = Integer.parseInt(gmOrders[1]);
                reqSwitchCasino(playerController, req);
            } else if ("addPower".equalsIgnoreCase(gmOrders[0])) {
                int num = Integer.parseInt(gmOrders[1]);
                SimPlayerContext context = this.simPlayerContextRegistry.getContext(playerController.playerId());
                context.getSimBaseData().setPower(num + context.getSimBaseData().getPower());
            } else if ("caLevel".equalsIgnoreCase(gmOrders[0])) {
                reqSimCasinoInfo(playerController, null);
            } else if ("genGuest".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerController.playerId());
                int num = Integer.parseInt(gmOrders[1]);
                if (num > 500 || num < 1) {
                    log.warn("单次生成游客数量区间在 1-500");
                    res.code = Code.FAIL;
                    return res;
                }
                //场景配置
                CasinoStatsSheetCfg casinoCfg = configCacheService.getCasinoStatsSheetCfg(ctx.getCurrentCasino().getCasinoId(), ctx.getCurrentCasino().getCasinoLevel());
                if (casinoCfg == null) {
                    log.warn("生成游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={},level={}", ctx.playerId(), ctx.getCurrentCasino().getCasinoId(), ctx.getCurrentCasino().getCasinoLevel());
                    res.code = Code.FAIL;
                    return res;
                }
                guestService.batchGenerateGuest(ctx, num, casinoCfg, System.currentTimeMillis(), null);
            } else if ("specifyIdGenGuest".equalsIgnoreCase(gmOrders[0])) {
                int id = Integer.parseInt(gmOrders[1]);
                int num = Integer.parseInt(gmOrders[2]);
                if (num > 500 || num < 1) {
                    log.warn("单次生成游客数量区间在 1-500");
                    res.code = Code.FAIL;
                    return res;
                }

                SimPlayerContext context = this.simPlayerContextRegistry.getContext(playerController.playerId());
                guestService.batchGenerateSpecifyIdGuest(context, id, num);
            } else if ("specifyQualityGenGuest".equalsIgnoreCase(gmOrders[0])) {
                int quality = Integer.parseInt(gmOrders[1]);
                int num = Integer.parseInt(gmOrders[2]);
                if (num > 500 || num < 1) {
                    log.warn("单次生成游客数量区间在 1-500");
                    res.code = Code.FAIL;
                    return res;
                }

                SimPlayerContext context = this.simPlayerContextRegistry.getContext(playerController.playerId());
                guestService.batchGenerateSpecifyQualityGuest(context, quality, num);
            } else if ("genPurchasedGuest".equalsIgnoreCase(gmOrders[0])) {
                int guestId = Integer.parseInt(gmOrders[1]);
                execute(playerController, ctx -> {
                    guestService.generatePurchasedGuest(ctx, guestId, 1);
                });
            } else if ("claimPurchasedGuest".equalsIgnoreCase(gmOrders[0])) {
                int index = Integer.parseInt(gmOrders[2]);
                execute(playerController, ctx -> {
                    guestService.claimPurchasedGuestReward(ctx, gmOrders[1], index);
                });
            } else if ("operationData".equalsIgnoreCase(gmOrders[0])) {
                reqOperationData(playerController, null);
            } else if ("slotStat".equalsIgnoreCase(gmOrders[0])) {
                ReqSlotStat req = new ReqSlotStat();
                req.gameType = gmOrders.length > 1 ? Integer.parseInt(gmOrders[1]) : 0;
                reqSlotStat(playerController, req);
            } else if ("coopList".equalsIgnoreCase(gmOrders[0])) {
                reqCoopTaskList(playerController, null);
            } else if ("coopRefresh".equalsIgnoreCase(gmOrders[0])) {
                reqCoopTaskRefresh(playerController, null);
            } else if ("coopClaim".equalsIgnoreCase(gmOrders[0])) {
                ReqCoopTaskClaim req = new ReqCoopTaskClaim();
                req.taskId = Integer.parseInt(gmOrders[1]);
                reqCoopTaskClaim(playerController, req);
            } else if ("coopReward".equalsIgnoreCase(gmOrders[0])) {
                ReqCoopTaskReward req = new ReqCoopTaskReward();
                req.taskId = Integer.parseInt(gmOrders[1]);
                reqCoopTaskReward(playerController, req);
            } else if ("coopSettle".equalsIgnoreCase(gmOrders[0])) {
                //模拟结算回写 (不经房间, 联调任务态/奖励闭环): coopSettle <taskId> <0失败|1成功>
                int taskId = Integer.parseInt(gmOrders[1]);
                boolean success = gmOrders.length > 2 && "1".equals(gmOrders[2]);
                execute(playerController, ctx -> {
                    //未建房的已领取任务先补 IN_ROOM 态, 满足结算状态机
                    com.jjg.game.sim.data.SimCoopTaskEntry entry = ctx.getSimCoopTaskData() == null
                            ? null : ctx.getSimCoopTaskData().getTasks().get(taskId);
                    if (entry != null && entry.getStatus() == com.jjg.game.sim.constant.CoopTaskConst.TaskStatus.CLAIMED) {
                        coopTaskService.markRoomCreated(ctx, taskId, -1L, 0);
                    }
                    long roomId = entry == null ? 0L : entry.getRoomId();
                    coopTaskService.onSettle(ctx, ctx.playerId(), taskId, roomId, success, java.util.List.of());
                });
            } else if ("casinoLevelUp".equalsIgnoreCase(gmOrders[0])) {
                int statsId = Integer.parseInt(gmOrders[1]);
                CasinoStatsSheetCfg cfg = GameDataManager.getCasinoStatsSheetCfg(statsId);
                if (cfg == null) {
                    res.code = Code.FAIL;
                    log.warn("未找到该配置 statsId={}", statsId);
                    return res;
                }
                SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerController.playerId());

                int oldLevel = ctx.getCurrentCasino().getCasinoLevel();
                ctx.getCurrentCasino().setCasinoLevel(cfg.getLevel());
                ctx.getSimBaseData().addAllLevel(ctx.getCurrentCasino().getCasinoLevel() - oldLevel);
                guideService.triggerSceneTotalLevelReached(ctx, ctx.getSimBaseData().getAllLevel(), true);
                ctx.setLastSaveTime(0);
            } else if ("simBuildLevelUp".equalsIgnoreCase(gmOrders[0])) {
                int buildingId = Integer.parseInt(gmOrders[1]);
                int level = Integer.parseInt(gmOrders[2]);
                if (buildingId < 1 || level < 1) {
                    res.code = Code.PARAM_ERROR;
                    log.warn("simBuildLevelUp 参数错误 buildingId={},level={}", buildingId, level);
                    return res;
                }
                SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerController.playerId());
                BuildingData buildingData = ctx.getCurrentCasino().findBuilding(buildingId);
                if (buildingData == null) {
                    res.code = Code.PARAM_ERROR;
                    log.warn("simBuildLevelUp 未找到该建筑 buildingId={},level={}", buildingId, level);
                    return res;
                }
                buildingData.setLevel(level);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("GM 指令执行异常 playerId={},orders={}",
                    playerController.playerId(), Arrays.toString(gmOrders), e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    public <T extends AbstractResponse> void execute(PlayerController pc, Consumer<SimPlayerContext> action) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(pc.playerId());
        if (ctx == null) {
            log.warn("获取ctx为空 playerId={}", pc.playerId());
            return;
        }
        action.accept(ctx);
    }

    private <T extends AbstractResponse> void executeVisit(PlayerController pc,
                                                           Function<SimPlayerContext, T> action,
                                                           IntFunction<T> exceptionResponse) {
        execute(pc, ctx -> {
            try {
                ctx.send(action.apply(ctx));
            } catch (Exception e) {
                log.error("处理拜访请求异常 playerId={}", pc.playerId(), e);
                ctx.send(exceptionResponse.apply(Code.EXCEPTION));
            }
        });
    }

    private <T extends AbstractResponse> void sendVisit(PlayerController pc, Supplier<T> action,
                                                        IntFunction<T> exceptionResponse) {
        try {
            pc.send(action.get());
        } catch (Exception e) {
            log.error("处理拜访请求异常 playerId={}", pc.playerId(), e);
            pc.send(exceptionResponse.apply(Code.EXCEPTION));
        }
    }
}
