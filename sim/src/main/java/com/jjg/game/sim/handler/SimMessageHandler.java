package com.jjg.game.sim.handler;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.FinishGuideRpcResult;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SkipGuideGroupRpcResult;
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
import java.util.concurrent.TimeUnit;

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
    private SimOnlineRewardService onlineRewardService;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private SimSkillService skillService;
    @Autowired
    private SimCasinoService casinoService;
    @Autowired
    private SimStatsService statsService;
    @Autowired
    private SimOperationDashboardService operationDashboardService;
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
    private SimGuideLogger simGuideLogger;
    @Autowired
    private SimGuideService guideService;
    @Autowired
    private SimNodeService simNodeService;
    @Autowired
    private RedDotManager redDotManager;
    @ClusterRpcReference
    private ToSimBridge toSimBridge;


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
        long playerId = playerController.playerId();
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            FinishGuideRpcResult result = finishGuideRemotely(playerController, req.guideId);
            ResFinishGuide response = new ResFinishGuide(result.code);
            response.guideId = result.guideId;
            playerController.send(response);
            notifyGuideTriggersDelayedOnCurrentNode(
                    playerController, result.triggeredGuideGroupIds);
            return;
        }
        SimGuideService.FinishGuideResult result = simManager.onFinishGuideWithTriggers(playerId, req.guideId);
        // 先发送完成响应，再稍作延迟通知由条件8触发的新引导组，给前端留出上一组的结束表现时间。
        playerController.send(result.response());
        simManager.notifyGuideTriggersDelayed(playerId, result.triggeredGuideGroupIds());
    }

    /**
     * 跳过整个新手引导组。
     */
    @Command(SimConstant.MsgBean.REQ_SKIP_GUIDE_GROUP)
    public void reqSkipGuideGroup(PlayerController playerController, ReqSkipGuideGroup req) {
        long playerId = playerController.playerId();
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            SkipGuideGroupRpcResult result = skipGuideGroupRemotely(playerController, req.guideGroupId);
            ResSkipGuideGroup response = new ResSkipGuideGroup(result.code);
            response.guideGroupId = result.guideGroupId;
            response.completedGuideIds = result.completedGuideIds;
            playerController.send(response);
            notifyGuideTriggersDelayedOnCurrentNode(
                    playerController, result.triggeredGuideGroupIds);
            return;
        }
        SimGuideService.SkipGuideGroupResult result =
                simManager.onSkipGuideGroup(playerId, req.guideGroupId);
        // 先返回跳过结果，再延迟通知由条件8触发的下一引导组。
        playerController.send(result.response());
        simManager.notifyGuideTriggersDelayed(playerId, result.triggeredGuideGroupIds());
    }

    private FinishGuideRpcResult finishGuideRemotely(PlayerController playerController, int guideId) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("转发完成新手引导失败，未找到玩家 sim 节点 playerId={},guideId={}", playerId, guideId);
            return new FinishGuideRpcResult(Code.NOT_FOUND, guideId, List.of());
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            FinishGuideRpcResult result = toSimBridge.finishGuide(playerId, guideId);
            return result == null
                    ? new FinishGuideRpcResult(Code.EXCEPTION, guideId, List.of()) : result;
        } catch (Exception e) {
            log.error("跨节点完成新手引导异常 playerId={},guideId={}", playerId, guideId, e);
            return new FinishGuideRpcResult(Code.EXCEPTION, guideId, List.of());
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private SkipGuideGroupRpcResult skipGuideGroupRemotely(PlayerController playerController,
                                                           int guideGroupId) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("转发跳过新手引导组失败，未找到玩家 sim 节点 playerId={},groupId={}",
                    playerId, guideGroupId);
            return new SkipGuideGroupRpcResult(Code.NOT_FOUND, guideGroupId, List.of(), List.of());
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            SkipGuideGroupRpcResult result = toSimBridge.skipGuideGroup(playerId, guideGroupId);
            return result == null
                    ? new SkipGuideGroupRpcResult(Code.EXCEPTION, guideGroupId, List.of(), List.of()) : result;
        } catch (Exception e) {
            log.error("跨节点跳过新手引导组异常 playerId={},groupId={}", playerId, guideGroupId, e);
            return new SkipGuideGroupRpcResult(Code.EXCEPTION, guideGroupId, List.of(), List.of());
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    /**
     * 在 poker 等当前游戏节点延迟发送条件8触发通知，保持响应先于通知。
     */
    private void notifyGuideTriggersDelayedOnCurrentNode(PlayerController playerController,
                                                         List<Integer> guideGroupIds) {
        if (guideGroupIds == null || guideGroupIds.isEmpty()) {
            return;
        }
        long playerId = playerController.playerId();
        List<Integer> groupSnapshot = List.copyOf(guideGroupIds);
        WheelTimerUtil.schedule(() ->
                        PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                                playerId, 0, new BaseHandler<String>() {
                                    @Override
                                    public void action() {
                                        if (playerController.getSession() == null
                                                || playerController.getSession().getReference() != playerController) {
                                            log.info("跳过非当前节点的新手引导组通知 playerId={},groups={}",
                                                    playerId, groupSnapshot);
                                            return;
                                        }
                                        NotifyGuideTrigger notify = new NotifyGuideTrigger(Code.SUCCESS);
                                        notify.guideGroupIds = groupSnapshot;
                                        playerController.send(notify);
                                        log.info("当前游戏节点延迟发送新手引导组通知 playerId={},delayMs={},groups={}",
                                                playerId,
                                                SimConstant.GuideTiming.GROUP_FINISH_NOTIFY_DELAY_MILLIS,
                                                groupSnapshot);
                                    }
                                }.setHandlerParamWithSelf("remote sim guide group finish delayed notify")),
                SimConstant.GuideTiming.GROUP_FINISH_NOTIFY_DELAY_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 客户端上报类型10的新手引导事件。
     * 玩家位于 poker 等非 SIM 节点时，转发到玩家所属 SIM 节点修改引导状态。
     */
    @Command(SimConstant.MsgBean.REQ_TRIGGER_GUIDE_EVENT)
    public void reqTriggerGuideEvent(PlayerController playerController, ReqTriggerGuideEvent req) {
        ResTriggerGuideEvent res = new ResTriggerGuideEvent(Code.SUCCESS);
        res.condition = req.condition;
        res.param = req.param;
        if (req.condition != SimConstant.GuideCondition.CLIENT_EVENT || req.param <= 0) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }

        long playerId = playerController.playerId();
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        CommonResult<List<Integer>> result = ctx == null
                ? triggerGuideEventRemotely(playerController, req.condition, req.param)
                : new CommonResult<>(Code.SUCCESS, guideService.triggerClientEvent(ctx, req.condition, req.param));
        if (result == null) {
            res.code = Code.EXCEPTION;
        } else {
            res.code = result.code;
        }
        // 先确认上报处理结果，再用原有通知协议启动本次首次命中的引导组。
        playerController.send(res);
        if (res.code == Code.SUCCESS && result.data != null && !result.data.isEmpty()) {
            NotifyGuideTrigger notify = new NotifyGuideTrigger(Code.SUCCESS);
            notify.guideGroupIds = result.data;
            playerController.send(notify);
        }
        log.info("处理客户端新手引导事件 playerId={},condition={},param={},code={},groups={}",
                playerId, req.condition, req.param, res.code,
                result == null ? null : result.data);
    }

    private CommonResult<List<Integer>> triggerGuideEventRemotely(PlayerController playerController,
                                                                  int condition, int param) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("转发客户端新手引导事件失败，未找到玩家 sim 节点 playerId={},condition={},param={}",
                    playerId, condition, param);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            CommonResult<List<Integer>> result = toSimBridge.triggerGuideEvent(playerId, condition, param);
            return result == null ? new CommonResult<>(Code.EXCEPTION) : result;
        } catch (Exception e) {
            log.error("跨节点处理客户端新手引导事件异常 playerId={},condition={},param={}",
                    playerId, condition, param, e);
            return new CommonResult<>(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    //--------------------------Casino相关 begin--------------------------

    /**
     * 开辟新场景
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_CASINO)
    public void reqUnlockCasino(PlayerController playerController, ReqUnlockCasino req) {
        execute(playerController, ctx -> {
            casinoService.onUnlockCasino(ctx, req.casinoId);
        }, ReqUnlockCasino.class);
    }

    /**
     * 切换场景
     */
    @Command(SimConstant.MsgBean.REQ_SWITCH_CASINO)
    public void reqSwitchCasino(PlayerController playerController, ReqSwitchCasino req) {
        execute(playerController, ctx -> {
            casinoService.onSwitchCasino(ctx, req.casinoId);
        }, ReqSwitchCasino.class);
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
        }, ReqSimCasinoInfo.class);
    }

    /**
     * 获取当前场景升级条件
     */
    @Command(SimConstant.MsgBean.REQ_CASINO_UPGRADE_CONDITION)
    public void reqCasinoUpgradeCondition(PlayerController playerController, ReqCasinoUpgradeCondition req) {
        execute(playerController, casinoService::onCasinoUpgradeCondition, ReqCasinoUpgradeCondition.class);
    }

    //--------------------------建筑相关 begin--------------------------

    /**
     * 解锁建筑
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_BUILDING)
    public void reqUnlockBuilding(PlayerController playerController, ReqUnlockBuilding req) {
        execute(playerController, ctx -> {
            ResUnlockBuilding res = buildingService.onUnlockBuilding(ctx, req.id);
            if (res != null) {
                ctx.send(res);
            }
        }, ReqUnlockBuilding.class);
    }

    /**
     * 获取建筑信息
     */
    @Command(SimConstant.MsgBean.REQ_BUILDING_INFO)
    public void reqBuildingInfo(PlayerController playerController, ReqBuildingInfo req) {
        execute(playerController, ctx -> {
            buildingService.onBuildingInfo(ctx, req.id);
        }, ReqBuildingInfo.class);
    }

    /**
     * 获取所有建筑信息
     */
    @Command(SimConstant.MsgBean.REQ_ALL_BUILDING_INFO)
    public void reqAllBuildingInfo(PlayerController playerController, ReqAllBuildingInfo req) {
        execute(playerController, ctx -> {
            buildingService.onAllBuildingInfo(ctx);
        }, ReqAllBuildingInfo.class);
    }

    /**
     * 升级建筑 (启动 CD)
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_BUILDING)
    public void reqUpgradeBuilding(PlayerController playerController, ReqUpgradeBuilding req) {
        execute(playerController, ctx -> {
            buildingService.onUpgradeBuilding(ctx, req.id);
        }, ReqUpgradeBuilding.class);
    }

    /**
     * 完成建筑升级
     */
    @Command(SimConstant.MsgBean.REQ_COMPLETE_BUILDING_UPGRADE)
    public void reqCompleteBuildingUpgrade(PlayerController playerController, ReqCompleteBuildingUpgrade req) {
        execute(playerController, ctx -> {
            buildingService.onCompleteBuildingUpgrade(ctx, req.id);
        }, ReqCompleteBuildingUpgrade.class);
    }

    /**
     * 清除升级 CD
     */
    @Command(SimConstant.MsgBean.REQ_CLEAR_BUILDING_CD)
    public void reqClearBuildingCD(PlayerController playerController, ReqClearBuildingCD req) {
        execute(playerController, ctx -> {
            buildingService.onClearBuildingCD(ctx, req.id, req.costCount, req.watchAd, req.costItemId);
        }, ReqClearBuildingCD.class);
    }

    /**
     * 领取离线收益
     */
    @Command(SimConstant.MsgBean.REQ_CLAIM_OFFLINE_REWARD)
    public void reqClaimOfflineReward(PlayerController playerController, ReqClaimOfflineReward req) {
        execute(playerController, ctx -> {
            buildingService.onClaimOfflineReward(ctx, req.watchAd);
        }, ReqClaimOfflineReward.class);
    }


    //--------------------------雇员相关 begin--------------------------

    /**
     * 招募雇员
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_EMPLOYEE)
    public void reqRecruitEmployee(PlayerController playerController, ReqRecruitEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onRecruitEmployee(ctx, req.poolId, req.count);
        }, ReqRecruitEmployee.class);
    }

    /**
     * 升级雇员
     */
    @Command(SimConstant.MsgBean.REQ_UPGRADE_EMPLOYEE)
    public void reqUpgradeEmployee(PlayerController playerController, ReqUpgradeEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onUpgradeEmployee(ctx, req.employeeId);
        }, ReqUpgradeEmployee.class);
    }

    /**
     * 升星雇员
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_EMPLOYEE)
    public void reqStarUpEmployee(PlayerController playerController, ReqStarUpEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onStarUpEmployee(ctx, req.employeeId);
        }, ReqStarUpEmployee.class);
    }

    /**
     * 任命/更换主管
     */
    @Command(SimConstant.MsgBean.REQ_ASSIGN_SUPERVISOR)
    public void reqAssignSupervisor(PlayerController playerController, ReqAssignSupervisor req) {
        execute(playerController, ctx -> {
            employeeService.onAssignSupervisor(ctx, req.employeeId);
        }, ReqAssignSupervisor.class);
    }


    /**
     * 获取所有雇员
     */
    @Command(SimConstant.MsgBean.REQ_ALL_EMPLOYEE)
    public void reqAllEmployee(PlayerController playerController, ReqAllEmployee req) {
        execute(playerController, ctx -> {
            employeeService.onAllEmployee(ctx);
        }, ReqAllEmployee.class);
    }

    /**
     * 获取雇员卡池
     */
    @Command(SimConstant.MsgBean.REQ_EMPLOYEE_POOL)
    public void reqEmployeePool(PlayerController playerController, ReqEmployeePool req) {
        execute(playerController, ctx -> {
            employeeService.onPool(ctx, req.poolId);
        }, ReqEmployeePool.class);
    }

    //--------------------------雇员相关 end--------------------------


    @Command(SimConstant.MsgBean.REQ_SIM_GET_SKILLS)
    public void reqSlotsGetSkills(PlayerController playerController, ReqSimGetSkills req) {
        execute(playerController, ctx -> {
            skillService.onLoadSlotsSkills(ctx, req.gameType);
        }, ReqSimGetSkills.class);
    }

    @Command(SimConstant.MsgBean.REQ_SIM_UPGRADE_SKILL)
    public void reqSimUpgradeSkill(PlayerController playerController, ReqSimUpgradeSkill req) {
        execute(playerController, ctx -> {
            skillService.onUpgradeSkill(ctx, req.gameType, req.skillId);
        }, ReqSimUpgradeSkill.class);
    }

    //--------------------------游客相关 begin--------------------------

    /**
     * 生成购买游客
     */
    @Command(SimConstant.MsgBean.REQ_GEN_PURCHASED_GUEST)
    public void reqGenPurchasedGuest(PlayerController playerController, ReqGenPurchasedGuest req) {
        execute(playerController, ctx -> {
//            guestService.generatePurchasedGuest(ctx, req.guestId, req.count);
        }, ReqGenPurchasedGuest.class);
    }

    /**
     * 领取购买游客奖励 (凭 uid 结算奖励)
     */
    @Command(SimConstant.MsgBean.REQ_PURCHASED_GUEST_REWARD)
    public void reqPurchasedGuestReward(PlayerController playerController, ReqPurchasedGuestReward req) {
        execute(playerController, ctx -> {
            guestService.claimPurchasedGuestReward(ctx, req.uid, req.index);
        }, ReqPurchasedGuestReward.class);
    }

    /**
     * 获取所有游客
     */
    @Command(SimConstant.MsgBean.REQ_ALL_GUEST)
    public void reqAllGuest(PlayerController playerController, ReqAllGuest req) {
        execute(playerController, ctx -> {
            guestService.onAllGuest(ctx);
        }, ReqAllGuest.class);
    }

    /**
     * 招募游客
     */
    @Command(SimConstant.MsgBean.REQ_RECRUIT_GUEST)
    public void reqRecruitGuest(PlayerController playerController, ReqRecruitGuest req) {
        execute(playerController, ctx -> {
            guestService.onRecruitGuest(ctx, req.poolId, req.count);
        }, ReqRecruitGuest.class);
    }

    /**
     * 获取开启的卡池
     */
    @Command(SimConstant.MsgBean.REQ_OPEN_POOL_LIST)
    public void reqOpenPoolList(PlayerController playerController, ReqOpenPoolList req) {
        execute(playerController, ctx -> {
            ResOpenPoolList res = new ResOpenPoolList(Code.SUCCESS);
            res.poolIds = configCacheService.getOpenPoolIds(req.poolType);
            ctx.send(res);
        }, ReqOpenPoolList.class);
    }

    /**
     * 升星游客
     */
    @Command(SimConstant.MsgBean.REQ_STAR_UP_GUEST)
    public void reqStarUpGuest(PlayerController playerController, ReqStarUpGuest req) {
        execute(playerController, ctx -> {
            guestService.onStarUpGuest(ctx, req.guestId);
        }, ReqStarUpGuest.class);
    }

    /**
     * 游客羁绊
     */
    @Command(SimConstant.MsgBean.REQ_UNLOCK_BONDS)
    public void reqUnlockBonds(PlayerController playerController, ReqGuestBonds req) {
        execute(playerController, ctx -> {
            List<Integer> viewedBondIds = guestService.onBonds(ctx);
            if (!viewedBondIds.isEmpty()) {
                // 客户端请求并成功收到羁绊详情，即认为本次返回的羁绊已经查看。
                redDotManager.markRead(playerController, RedDotDetails.RedDotModule.EMPLOYEE,
                        SimEmployeeRedDotService.NEW_BOND, viewedBondIds);
            }
        }, ReqGuestBonds.class);
    }

    /**
     * 获取游客卡池
     */
    @Command(SimConstant.MsgBean.REQ_GUEST_POOL)
    public void reqSimPool(PlayerController playerController, ReqGuestPool req) {
        execute(playerController, ctx -> {
            guestService.onPool(ctx, req.poolId);
        }, ReqGuestPool.class);
    }

    /**
     * 获取特殊游客列表
     */
    @Command(SimConstant.MsgBean.REQ_SPECIAL_GUEST_LIST)
    public void reqSpecialGuestList(PlayerController playerController, ReqSpecialGuestList req) {
        execute(playerController, ctx -> {
            guestService.specialGuests(ctx);
        }, ReqSpecialGuestList.class);
    }

    /**
     * 刷新特殊游客列表
     */
    @Command(SimConstant.MsgBean.REQ_REFRESH_SPECIAL_GUEST_LIST)
    public void reqRefreshSpecialGuestList(PlayerController playerController, ReqRefreshSpecialGuestList req) {
        execute(playerController, ctx -> {
            guestService.refreshSpecialGuests(ctx);
        }, ReqRefreshSpecialGuestList.class);
    }

    /**
     * 购买特殊游客
     */
    @Command(SimConstant.MsgBean.REQ_BUY_SPECIAL_GUEST)
    public void reqBuySpecialGuest(PlayerController playerController, ReqBuySpecialGuest req) {
        execute(playerController, ctx -> {
            guestService.buySpecialGuest(ctx, req.id, req.costType, req.payType);
        }, ReqBuySpecialGuest.class);
    }

    /**
     * 获取当前场景已购买的特殊游客
     */
    @Command(SimConstant.MsgBean.REQ_OWNED_SPECIAL_GUEST_LIST)
    public void reqOwnedSpecialGuestList(PlayerController playerController, ReqOwnedSpecialGuestList req) {
        execute(playerController, ctx -> {
            guestService.ownedSpecialGuests(ctx);
        }, ReqOwnedSpecialGuestList.class);
    }

    /**
     * 邀请特殊游客
     */
    @Command(SimConstant.MsgBean.REQ_INVITE_SPECIAL_GUEST)
    public void reqInviteSpecialGuest(PlayerController playerController, ReqInviteSpecialGuest req) {
        execute(playerController, ctx -> {
            guestService.inviteSpecialGuests(ctx, req.itemIds);
        }, ReqInviteSpecialGuest.class);
    }
    //--------------------------游客相关 end--------------------------

    /** 获取在线收益信息。 */
    @Command(SimConstant.MsgBean.REQ_SIM_ONLINE_REWARD)
    public void reqSimOnlineReward(PlayerController playerController, ReqSimOnlineReward req) {
        execute(playerController, ctx -> ctx.send(onlineRewardService.getInfo(ctx)), ReqSimOnlineReward.class);
    }

    /** 领取在线收益。 */
    @Command(SimConstant.MsgBean.REQ_SIM_CLAIM_ONLINE_REWARD)
    public void reqSimClaimOnlineReward(PlayerController playerController, ReqSimClaimOnlineReward req) {
        execute(playerController, ctx -> ctx.send(onlineRewardService.claim(ctx, req.type)),
                ReqSimClaimOnlineReward.class);
    }

    //--------------------------经营信息 begin--------------------------

    /**
     * 经营信息-运营数据
     */
    @Command(SimConstant.MsgBean.REQ_OPERATION_DATA)
    public void reqOperationData(PlayerController playerController, ReqOperationData req) {
        execute(playerController, ctx -> {
            statsService.onOperationData(ctx);
        }, ReqOperationData.class);
    }

    /**
     * 新版细分运营数据看板完整数据。
     */
    @Command(SimConstant.MsgBean.REQ_OPERATION_DASHBOARD)
    public void reqOperationDashboard(PlayerController playerController, ReqOperationDashboard req) {
        execute(playerController, operationDashboardService::onDashboard, ReqOperationDashboard.class);
    }

    /**
     * 新版细分运营数据看板实时容纳人数。
     */
    @Command(SimConstant.MsgBean.REQ_OPERATION_CAPACITY)
    public void reqOperationCapacity(PlayerController playerController, ReqOperationCapacity req) {
        execute(playerController, operationDashboardService::onCapacity, ReqOperationCapacity.class);
    }

    /**
     * 经营信息-SPINE游戏数据 (>0指定游戏, 0所有游戏汇总)
     */
    @Command(SimConstant.MsgBean.REQ_SLOT_STAT)
    public void reqSlotStat(PlayerController playerController, ReqSlotStat req) {
        execute(playerController, ctx -> {
            statsService.onSlotStat(ctx, req.gameType);
        }, ReqSlotStat.class);
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
     * 获取主线任务列表
     */
    @Command(SimConstant.MsgBean.REQ_SIM_TASK_LIST)
    public void reqSimTaskList(PlayerController playerController, ReqSimTaskList req) {
        execute(playerController, ctx -> ctx.send(taskService.buildTaskList(ctx)), ReqSimTaskList.class);
    }

    /**
     * 获取指定建筑的成就任务列表
     */
    @Command(SimConstant.MsgBean.REQ_SIM_ACHIEVEMENT_TASK_LIST)
    public void reqSimAchievementTaskList(PlayerController playerController, ReqSimAchievementTaskList req) {
        execute(playerController, ctx -> ctx.send(taskService.buildAchievementTaskList(ctx, req.buildingId)),
                ReqSimAchievementTaskList.class);
    }

    /**
     * 领取任务奖励
     */
    @Command(SimConstant.MsgBean.REQ_SIM_TASK_REWARD)
    public void reqSimTaskReward(PlayerController playerController, ReqSimTaskReward req) {
        long playerId = playerController.playerId();
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        ResSimTaskReward response = ctx == null
                ? claimTaskRewardRemotely(playerController, req.taskId)
                : taskService.claimReward(ctx, req.taskId);
        playerController.send(response);
    }

    private ResSimTaskReward claimTaskRewardRemotely(PlayerController playerController, int taskId) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("领取 sim 任务奖励失败，未找到玩家 sim 节点 playerId={},taskId={}", playerId, taskId);
            return new ResSimTaskReward(Code.NOT_FOUND);
        }

        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            ResSimTaskReward response = toSimBridge.claimSimTaskReward(playerId, taskId);
            return response == null ? new ResSimTaskReward(Code.EXCEPTION) : response;
        } catch (Exception e) {
            log.error("跨节点领取 sim 任务奖励异常 playerId={},taskId={}", playerId, taskId, e);
            return new ResSimTaskReward(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    /**
     * 成就勋章面板 (达成统计/全服排行/品质统计/加成档)
     */
    @Command(SimConstant.MsgBean.REQ_MEDAL_PANEL)
    public void reqMedalPanel(PlayerController playerController, ReqMedalPanel req) {
        execute(playerController, ctx -> ctx.send(medalService.buildMedalPanel(ctx)), ReqMedalPanel.class);
    }

    /**
     * 修改场景与个人简介共用的展示徽章
     */
    @Command(SimConstant.MsgBean.REQ_CHANGE_SHOW_MEDAL)
    public void reqChangeShowMedal(PlayerController playerController, ReqChangeShowMedal req) {
        execute(playerController, ctx -> ctx.send(medalService.changeShowMedal(ctx, req.medalIds)),
                ReqChangeShowMedal.class);
    }

    //--------------------------任务 (主线/成就) end--------------------------

    //--------------------------多人协作任务 begin--------------------------

    /**
     * 多人任务今日列表
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_LIST)
    public void reqCoopTaskList(PlayerController playerController, ReqCoopTaskList req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.buildTaskList(ctx)), ReqCoopTaskList.class);
    }

    /**
     * 刷新多人任务列表 (每日首免, 之后耗道具)
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_REFRESH)
    public void reqCoopTaskRefresh(PlayerController playerController, ReqCoopTaskRefresh req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.refresh(ctx)), ReqCoopTaskRefresh.class);
    }

    /**
     * 领取多人任务
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_CLAIM)
    public void reqCoopTaskClaim(PlayerController playerController, ReqCoopTaskClaim req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.claim(ctx, req.taskId)), ReqCoopTaskClaim.class);
    }

    /**
     * 发起者领取多人任务奖励
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_REWARD)
    public void reqCoopTaskReward(PlayerController playerController, ReqCoopTaskReward req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.claimReward(ctx, req.taskId)),
                ReqCoopTaskReward.class);
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
        }, ReqCreateCoopRoom.class);
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
        }, ReqJoinCoopRoom.class);
    }

    /**
     * 获取战力
     */
    @Command(SimConstant.MsgBean.REQ_COMBAT_POWER)
    public void reqCombatPower(PlayerController playerController, ReqCombatPower req) {
        execute(playerController, ctx -> ctx.send(coopTaskService.combatPowers(ctx, req.playerId)),
                ReqCombatPower.class);
    }

    /**
     * 批量获取多人任务当前人数
     */
    @Command(SimConstant.MsgBean.REQ_COOP_TASK_MEMBERS)
    public void reqCoopTaskMembers(PlayerController playerController, ReqCoopTaskMembers req) {
        execute(playerController, ctx -> ctx.send(coopRoomRouteService.memberCounts(req.members)),
                ReqCoopTaskMembers.class);
    }

    //--------------------------多人协作任务 end--------------------------

    //--------------------------拜访相关 begin--------------------------

    @Command(SimConstant.MsgBean.REQ_VISIT_CASINO)
    public void reqVisitCasino(PlayerController playerController, ReqVisitCasino req) {
        long playerId = playerController.playerId();
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            executeVisit(playerController, context -> visitService.visit(context, req.playerId, req.casinoId),
                    ResVisitCasino::new, ReqVisitCasino.class);
            return;
        }
        playerController.send(visitCasinoRemotely(playerController, req.playerId, req.casinoId));
    }

    private ResVisitCasino visitCasinoRemotely(PlayerController playerController,
                                                long targetPlayerId, int casinoId) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("跨节点拜访赌场失败，未找到玩家 sim 节点 playerId={},targetPlayerId={},casinoId={}",
                    playerId, targetPlayerId, casinoId);
            return new ResVisitCasino(Code.NOT_FOUND);
        }

        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            ResVisitCasino response = toSimBridge.visitCasino(playerId, targetPlayerId, casinoId);
            return response == null ? new ResVisitCasino(Code.EXCEPTION) : response;
        } catch (Exception e) {
            log.error("跨节点拜访赌场异常 playerId={},targetPlayerId={},casinoId={}",
                    playerId, targetPlayerId, casinoId, e);
            return new ResVisitCasino(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    @Command(SimConstant.MsgBean.REQ_RANDOM_VISIT)
    public void reqRandomVisit(PlayerController playerController, ReqRandomVisit req) {
        executeVisit(playerController, ctx -> visitService.randomVisit(ctx, req.lastPlayerId),
                ResVisitCasino::new, ReqRandomVisit.class);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_LIKE)
    public void reqVisitLike(PlayerController playerController, ReqVisitLike req) {
        executeVisit(playerController, ctx -> visitService.like(ctx, req.playerId, req.casinoId),
                ResVisitAction::new, ReqVisitLike.class);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_COMMENT)
    public void reqVisitComment(PlayerController playerController, ReqVisitComment req) {
        executeVisit(playerController,
                ctx -> visitService.comment(ctx, req.playerId, req.casinoId, req.content),
                ResVisitAction::new, ReqVisitComment.class);
    }

    @Command(SimConstant.MsgBean.REQ_VISIT_GIFT)
    public void reqVisitGift(PlayerController playerController, ReqVisitGift req) {
        executeVisit(playerController,
                ctx -> visitService.gift(ctx, req.playerId, req.casinoId, req.giftId),
                ResVisitAction::new, ReqVisitGift.class);
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
                ResVisitComments::new, ReqVisitComments.class);
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
        }, ReqEnterVisitGame.class);
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
            } else if ("jumpTask".equalsIgnoreCase(gmOrders[0])) {
                if (gmOrders.length < 2 || !gmOrders[1].matches("\\d+")) {
                    res.code = Code.PARAM_ERROR;
                    res.data = "参数错误，格式：jumpTask <taskId>";
                    return res;
                }
                int taskId = Integer.parseInt(gmOrders[1]);
                if (taskId <= 0) {
                    res.code = Code.PARAM_ERROR;
                    res.data = "taskId 必须大于0";
                    return res;
                }
                SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerController.playerId());
                CommonResult<String> stringCommonResult = taskService.jumpTask(ctx, taskId);
                return stringCommonResult;
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
                }, String[].class);
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
                req.poolId = Integer.parseInt(gmOrders[1]);
                req.count = gmOrders.length > 2 ? Integer.parseInt(gmOrders[2]) : 1;
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
                }, String[].class);
            } else if ("specialGuestList".equalsIgnoreCase(gmOrders[0])) {
                // specialGuestList 0；GM 总入口要求至少携带一个参数，参数值不使用。
                reqSpecialGuestList(playerController, new ReqSpecialGuestList());
                res.data = "已请求特殊游客列表";
            } else if ("refreshSpecialGuestList".equalsIgnoreCase(gmOrders[0])) {
                // refreshSpecialGuestList 0
                reqRefreshSpecialGuestList(playerController, new ReqRefreshSpecialGuestList());
                res.data = "已请求刷新特殊游客列表";
            } else if ("buySpecialGuest".equalsIgnoreCase(gmOrders[0])) {
                // buySpecialGuest <生成配置ID> [支付方式]；支付方式仅现金类型使用。
                ReqBuySpecialGuest req = new ReqBuySpecialGuest();
                req.id = Integer.parseInt(gmOrders[1]);
                req.costType = Integer.parseInt(gmOrders[2]);
                req.payType = gmOrders.length > 3 ? Integer.parseInt(gmOrders[3]) : 0;
                reqBuySpecialGuest(playerController, req);
                res.data = "已请求购买特殊游客";
            } else if ("ownedSpecialGuestList".equalsIgnoreCase(gmOrders[0])) {
                // ownedSpecialGuestList 0
                reqOwnedSpecialGuestList(playerController, new ReqOwnedSpecialGuestList());
                res.data = "已请求当前场景已购买特殊游客列表";
            } else if ("inviteSpecialGuest".equalsIgnoreCase(gmOrders[0])) {
                // inviteSpecialGuest <游客道具ID,游客道具ID...>
                ReqInviteSpecialGuest req = new ReqInviteSpecialGuest();
                req.itemIds = Arrays.stream(gmOrders[1].split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .toList();
                reqInviteSpecialGuest(playerController, req);
                res.data = "已请求邀请特殊游客：" + req.itemIds;
            } else if ("claimPurchasedGuest".equalsIgnoreCase(gmOrders[0])) {
                int index = Integer.parseInt(gmOrders[2]);
                execute(playerController, ctx -> {
                    guestService.claimPurchasedGuestReward(ctx, gmOrders[1], index);
                }, String[].class);
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
                }, String[].class);
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
                casinoService.addAllLevel(ctx, ctx.getCurrentCasino().getCasinoLevel() - oldLevel);
                guideService.triggerSceneTotalLevelReached(ctx, ctx.getSimBaseData().getAllLevel(), true);
                taskService.onConditionEvent(ctx,
                        SimConditionEventFactory.sceneLevel(ctx.getCurrentCasino().getCasinoId(),
                                ctx.getCurrentCasino().getCasinoLevel()));
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
            } else if ("simAddExp".equalsIgnoreCase(gmOrders[0])) {
                int exp = Integer.parseInt(gmOrders[1]);
                SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerController.playerId());
                casinoService.addCasinoExp(ctx, exp);
            } else if ("unlockAllBuild".equalsIgnoreCase(gmOrders[0])) {
                SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerController.playerId());
                buildingService.gmUnlockAllBuilds(ctx);
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

    public void execute(PlayerController pc, Consumer<SimPlayerContext> action, Class<?> requestClass) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(pc.playerId());
        if (ctx == null) {
            log.warn("获取ctx为空 playerId={},request={}", pc.playerId(), requestClass.getSimpleName());
            return;
        }
        action.accept(ctx);
    }

    private <T extends AbstractResponse> void executeVisit(PlayerController pc,
                                                           Function<SimPlayerContext, T> action,
                                                           IntFunction<T> exceptionResponse,
                                                           Class<?> requestClass) {
        execute(pc, ctx -> {
            try {
                ctx.send(action.apply(ctx));
            } catch (Exception e) {
                log.error("处理拜访请求异常 playerId={}", pc.playerId(), e);
                ctx.send(exceptionResponse.apply(Code.EXCEPTION));
            }
        }, requestClass);
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
