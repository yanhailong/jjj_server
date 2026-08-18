package com.jjg.game.hall.handler;

import com.jjg.game.alliance.bridge.ToAllianceBridge;
import com.jjg.game.alliance.service.AllianceCacheService;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.rpc.GmToHallBridge;
import com.jjg.game.core.rpc.SpecialGuestBridge;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ResearchSkillsCfg;
import com.jjg.game.season.data.SeasonFreeSpinResult;
import com.jjg.game.season.data.SeasonSlotsSessionData;
import com.jjg.game.season.pb.res.ResSeasonMatch;
import com.jjg.game.season.pb.res.ResSeasonTrialProgress;
import com.jjg.game.season.service.SeasonService;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.logger.SimGuideLogger;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.res.ResSimTaskReward;
import com.jjg.game.sim.service.SimCoopTaskService;
import com.jjg.game.sim.service.SimGuideService;
import com.jjg.game.sim.service.SimGuestService;
import com.jjg.game.sim.service.SimPackService;
import com.jjg.game.sim.service.SimSkillService;
import com.jjg.game.sim.service.SimTaskService;
import com.jjg.game.social.bridge.ToSocialBridge;
import com.jjg.game.social.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2026/1/19
 */
@Component
public class HallRPCController extends CoreRPCController implements GmToHallBridge, ToSimBridge, ToAllianceBridge,
        ToSocialBridge, SpecialGuestBridge {

    @Autowired
    private AccountDao accountDao;
    @Autowired
    private HallPlayerService playerService;
    @Autowired
    private HallService hallService;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private AllianceEventService allianceEventService;
    @Autowired
    private AllianceCacheService allianceCacheService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Autowired
    private SimManager simManager;
    @Autowired
    private SeasonService seasonService;
    @Autowired
    private SimCoopTaskService simCoopTaskService;
    @Autowired
    private SimTaskService simTaskService;
    @Autowired
    private SimPackService simPackService;
    @Autowired
    private SimGuideService simGuideService;
    @Autowired
    private SimGuestService simGuestService;
    @Autowired
    private SimGuideLogger simGuideLogger;
    @Autowired
    private ChatService chatService;

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> receiveSpecialGuest(long playerId, int casinoId, int itemId,
                                                     long count, String orderId) {
        boolean success = simGuestService.receiveCashSpecialGuest(
                playerId, casinoId, itemId, count, orderId);
        return new CommonResult<>(success ? Code.SUCCESS : Code.FAIL, success);
    }

    @Override
    public int playerBindPhone(long playerId, String phone, int type, boolean reward) {
        log.info("收到绑定或解绑手机请求 playerId = {},phone = {},type = {}", playerId, phone, type);
        int code = Code.SUCCESS;
        try {
            Player player = playerService.get(playerId);
            if (player == null) {
                log.warn("后台绑定或解绑手机时，未找到该玩家信息 playerId = {},phone = {},type = {}", playerId, phone, type);
                return Code.NOT_FOUND;
            }

            if (type == 1) {  //绑定
                return hallService.playerBindPhone(player, phone, reward).code;
            } else if (type == 2) {  //解绑
                CommonResult<Account> accountCommonResult = accountDao.removeThirdAccount(player, LoginType.PHONE);
                if (!accountCommonResult.success()) {
                    log.warn("绑定或解绑手机失败1 playerId = {},failCode = {}", player.getId(), accountCommonResult.code);
                    return accountCommonResult.code;
                }
                log.info("玩家解绑手机成功 playerId = {}", playerId, phone, type);
            } else {
                code = Code.FAIL;
                log.warn("不支持的绑定类型 playerId = {},phone = {},type = {}", playerId, phone, type);
            }
        } catch (Exception e) {
            log.error("", e);
            code = Code.EXCEPTION;
        }
        return code;
    }

    @Override
    public int afterVerifySmsSuccess(long playerId, String phone, int type) {
        try {
            log.info("大厅收到后台在短信验证成功后的消息 playerId = {},phone = {},type = {}", playerId, phone, type);
            VerCodeType verCodeType = VerCodeType.getType(type);
            if (verCodeType == null) {
                return Code.SUCCESS;
            }

            Player player = null;
            switch (verCodeType) {
                case SMS_BIND_PHONE:
                    player = playerService.get(playerId);
                    return hallService.playerBindPhone(player, phone, true).code;
                default:
                    return Code.SUCCESS;
            }
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public int finishSimGuide(long playerId, int operationType, List<Integer> guideIds) {
        int code;
        if (operationType == BackendGMCmd.SimGuideOperation.FINISH_ALL) {
            code = simManager.onGmFinishAllGuides(playerId);
        } else if (operationType == BackendGMCmd.SimGuideOperation.FINISH_SPECIFIED) {
            code = simManager.onGmFinishGuides(playerId, guideIds);
        } else {
            return Code.PARAM_ERROR;
        }
        if (code == Code.SUCCESS) {
            simGuideLogger.completed(playerId, operationType, guideIds);
        }
        log.info("后台完成新手引导处理结束 playerId={},operationType={},guideIds={},code={}",
                playerId, operationType, guideIds, code);
        return code;
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public FinishGuideRpcResult finishGuide(long playerId, int guideId) {
        SimGuideService.FinishGuideResult result = simManager.onFinishGuideWithTriggers(playerId, guideId);
        return new FinishGuideRpcResult(result.response().code, result.response().guideId,
                result.triggeredGuideGroupIds());
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public SkipGuideGroupRpcResult skipGuideGroup(long playerId, int guideGroupId) {
        SimGuideService.SkipGuideGroupResult result = simManager.onSkipGuideGroup(playerId, guideGroupId);
        return new SkipGuideGroupRpcResult(result.response().code, result.response().guideGroupId,
                result.response().completedGuideIds, result.triggeredGuideGroupIds());
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<SimSkillsData> addSkillById(long playerId, int gameType, int skillId) {
        ResearchSkillsCfg cfg = GameDataManager.getResearchSkillsCfg(skillId);
        if (cfg == null) {
            log.warn("添加技能失败，未找到技能配置 playerId={},skillId={}", playerId, skillId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("添加技能失败，未找到玩家 sim 数据 playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        SimSkillsData data = ctx.getSkillData(gameType);
        if (data == null) {
            log.warn("添加技能失败，该技能 playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        data.changeSkillLevel(cfg.getAttr(), cfg.getGrade());

        log.info("添加技能成功 playerId={},gameType={},skillId={},propId={},grade={}",
                playerId, gameType, skillId, cfg.getAttr(), cfg.getGrade());
        return new CommonResult<>(Code.SUCCESS, data);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<SimSkillsData> getSkillData(long playerId, int gameType) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            //在线: 以内存态为准 (定时落库前内存可能比 DB 新); 该游戏无技能返回 null 属正常
            return new CommonResult<>(Code.SUCCESS, ctx.getSkillData(gameType));
        }
        //离线: sim 内存无数据, 回退读库
        return new CommonResult<>(Code.SUCCESS, simSkillService.getSkillDataByGameType(playerId, gameType));
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode,
                                                     SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit, int enterType) {
        return simManager.onSlotsSpin(playerId, gameType, winTimes, changeNode, statInfo, trialPermit, enterType);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public ResSimTaskReward claimSimTaskReward(long playerId, int taskId) {
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("领取 sim 任务奖励失败，未找到玩家 sim 数据 playerId={},taskId={}", playerId, taskId);
            return new ResSimTaskReward(Code.NOT_FOUND);
        }
        return simTaskService.claimReward(ctx, taskId);
    }

    @Override
    public CommonResult<Boolean> onDouXianSettled(List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return new CommonResult<>(Code.PARAM_ERROR, false);
        }
        Set<Long> distinctPlayerIds = new LinkedHashSet<>(playerIds);
        distinctPlayerIds.removeIf(playerId -> playerId == null || playerId <= 0);
        if (distinctPlayerIds.isEmpty()) {
            return new CommonResult<>(Code.PARAM_ERROR, false);
        }
        for (long playerId : distinctPlayerIds) {
            PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                    playerId, 0, new BaseHandler<String>() {
                        @Override
                        public void action() {
                            SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
                            if (ctx == null) {
                                log.warn("斗仙牌结算任务推进失败，未找到玩家 sim 数据 playerId={}", playerId);
                                return;
                            }
                            simTaskService.onConditionEvent(ctx, new ActionConditionEvent(
                                    ActionConditionEvent.Type.DOUXIAN_SETTLEMENT,
                                    0, 0, 0, 1, 0, false));
                        }
                    }.setHandlerParamWithSelf("dou xian settlement task progress"));
        }
        return new CommonResult<>(Code.SUCCESS, true);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<List<Integer>> triggerGuideEvent(long playerId, int condition, int param) {
        if (condition != com.jjg.game.sim.constant.SimConstant.GuideCondition.CLIENT_EVENT || param <= 0) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("客户端新手引导事件处理失败，未找到玩家 sim 数据 playerId={},condition={},param={}",
                    playerId, condition, param);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        List<Integer> guideGroupIds = simGuideService.triggerClientEvent(ctx, condition, param);
        return new CommonResult<>(Code.SUCCESS, guideGroupIds);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public ResSeasonMatch seasonMatch(long playerId, int gameType, long stake) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("赛季匹配失败，未找到玩家 sim 数据 playerId={}", playerId);
            return new ResSeasonMatch(Code.NOT_FOUND);
        }
        return seasonService.match(ctx, gameType, stake);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public ResSeasonMatch passiveSeasonMatch(long playerId, int gameType, long stake, long excludedSpinId) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("赛季被动匹配失败，未找到玩家 sim 数据 playerId={}", playerId);
            return new ResSeasonMatch(Code.NOT_FOUND);
        }
        return seasonService.passiveMatch(ctx, gameType, stake, excludedSpinId);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public ResSeasonTrialProgress seasonTrialProgress(long playerId) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("获取赛季试炼进度失败，未找到玩家 sim 数据 playerId={}", playerId);
            return new ResSeasonTrialProgress(Code.NOT_FOUND);
        }
        return seasonService.trialProgress(ctx);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<String> seasonGm(long playerId, String[] orders) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("赛季 GM 执行失败，未找到玩家 sim 数据 playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        return seasonService.gmTime(ctx, orders);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<SeasonFreeSpinResult> useSeasonFreeSpin(long playerId, int gameType) {
        return simManager.useSeasonFreeSpin(playerId, gameType);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<SeasonSlotsSessionData> getSeasonSlotsSessionData(long playerId, int gameType) {
        return simManager.getSeasonSlotsSessionData(playerId, gameType);
    }

    @Override
    @Deprecated
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Long> getSeasonCoin(long playerId) {
        return simManager.getSeasonCoin(playerId);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Long> deductSeasonCoin(long playerId, long amount, long transactionId) {
        return simManager.deductSeasonCoin(playerId, amount, transactionId);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Long> addSeasonCoin(long playerId, long amount, long transactionId) {
        return simManager.addSeasonCoin(playerId, amount, transactionId);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(long playerId, int gameType, boolean freeMode) {
        return simManager.prepareVisitTrialSpin(playerId, gameType, freeMode);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> cancelVisitTrialSpin(long playerId, VisitTrialSpinPermit permit) {
        return simManager.cancelVisitTrialSpin(playerId, permit);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Map<Integer, Integer>> skillLevelUp(long playerId, int gameType, int skillId) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("技能升级失败，未找到SimPlayerContext playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        return simSkillService.skillLevelUp(ctx, gameType, skillId);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public int getCombatPower(long playerId) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            log.warn("获取战力失败，未找到SimPlayerContext playerId={}", playerId);
            return 0;
        }
        return simSkillService.computeCombatPower(ctx);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> addSimItems(long playerId, List<Item> items, AddType addType, String desc) {
        try {
            boolean added = simPackService.addItemsHere(playerId, items, addType);
            return new CommonResult<>(added ? Code.SUCCESS : Code.FAIL, added);
        } catch (Exception e) {
            log.error("跨节点入账sim特殊资源异常 playerId={},items={},addType={},desc={}", playerId, items, addType, desc, e);
            return new CommonResult<>(Code.EXCEPTION, false);
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> removeSimItems(long playerId, List<Item> items, AddType addType, String desc) {
        try {
            boolean removed = simPackService.removeItemsHere(playerId, items, addType);
            return new CommonResult<>(removed ? Code.SUCCESS : Code.FAIL, removed);
        } catch (Exception e) {
            log.error("跨节点扣除sim特殊资源异常 playerId={},items={},addType={},desc={}", playerId, items, addType, desc, e);
            return new CommonResult<>(Code.EXCEPTION, false);
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Long> getSimItemCount(long playerId, int itemId) {
        try {
            return new CommonResult<>(Code.SUCCESS, simPackService.getItemCountHere(playerId, itemId));
        } catch (Exception e) {
            log.error("跨节点读取sim特殊资源数量异常 playerId={},itemId={}", playerId, itemId, e);
            return new CommonResult<>(Code.EXCEPTION, 0L);
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> onPackItemsAdded(long playerId, Map<Integer, Long> items, AddType addType) {
        try {
            SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                return new CommonResult<>(Code.NOT_FOUND, false);
            }
            simGuideService.triggerItemsAdded(ctx, items);
            return new CommonResult<>(Code.SUCCESS, true);
        } catch (Exception e) {
            log.error("跨节点处理道具入账事件异常 playerId={},items={},addType={}", playerId, items, addType, e);
            return new CommonResult<>(Code.EXCEPTION, false);
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> onPackItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType) {
        try {
            if (simPlayerContextRegistry.getContext(playerId) == null) {
                return new CommonResult<>(Code.NOT_FOUND, false);
            }
            allianceEventService.onItemsConsumed(playerId, items, addType);
            return new CommonResult<>(Code.SUCCESS, true);
        } catch (Exception e) {
            log.error("跨节点处理道具消费事件异常 playerId={},items={},addType={}", playerId, items, addType, e);
            return new CommonResult<>(Code.EXCEPTION, false);
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public CommonResult<Boolean> onCoopRoomSettle(long ownerId, int taskId, long roomId, boolean success, List<Long> helperIds) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(ownerId);
            boolean settled = simCoopTaskService.onSettle(ctx, ownerId, taskId, roomId, success, helperIds);
            return new CommonResult<>(settled ? Code.SUCCESS : Code.FAIL, settled);
        } catch (Exception e) {
            log.error("多人任务结算回写异常 ownerId={},taskId={},success={}", ownerId, taskId, success, e);
            return new CommonResult<>(Code.EXCEPTION, false);
        }
    }

    // --------------------------- ToAllianceBridge (联盟跨节点入口) ---------------------------

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public void reportEarnGold(long playerId, int gameType, long gold) {
        allianceEventService.onEarnGold(playerId, gameType, gold);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public void reportAllianceEvent(long playerId, int conditionId, long param, long value) {
        allianceEventService.onEvent(playerId, conditionId, param, value);
    }

    @Override
    public long getPlayerAllianceId(long playerId) {
        return allianceCacheService.getAllianceId(playerId);
    }

    // --------------------------- ToSocialBridge (社交跨节点入口) ---------------------------

    @Override
    public void pushSystemMessage(String content) {
        chatService.sendSystemMessage(content);
    }

    @Override
    public void pushBigWin(long roomId, long playerId, String prizeName, long amount) {
        //预留: slots 房间频道接入后实现
        log.info("收到房间大奖播报(暂未接入房间频道) roomId={},playerId={},prize={},amount={}",
                roomId, playerId, prizeName, amount);
    }

    @Override
    public void pushRoomChat(long roomId, long playerId, String content) {
        //预留: slots 房间频道接入后实现
        log.info("收到房间聊天(暂未接入房间频道) roomId={},playerId={}", roomId, playerId);
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg0")
    public int sendCoopInvite(long senderId, int channelCode, long targetId, String content) {
        return chatService.sendChatFrom(senderId, channelCode, targetId, content);
    }
}
