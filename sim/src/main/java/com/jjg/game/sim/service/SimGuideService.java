package com.jjg.game.sim.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.ItemAddListener;
import com.jjg.game.core.listener.ItemNotEnoughListener;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimBaseData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.logger.SimGuideLogger;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.res.NotifyGuideTrigger;
import com.jjg.game.sim.pb.res.ResFinishGuide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 服务端按组触发引导，按 GuideId 保存具体步骤进度。 */
@Service
public class SimGuideService implements ItemAddListener, ItemNotEnoughListener {
    private static final Logger log = LoggerFactory.getLogger(SimGuideService.class);

    @Autowired
    private SimGuideConfigService configService;
    @Autowired
    private SimPlayerContextRegistry contextRegistry;
    @Autowired
    private SimGuideLogger guideLogger;
    @Lazy
    @Autowired
    private SimPackService simPackService;

    public List<Integer> trigger(SimPlayerContext ctx, int condition, int param, boolean notify) {
        if (ctx == null || ctx.getSimBaseData() == null) {
            return Collections.emptyList();
        }
        List<Integer> groups = configService.groupsFor(condition, param);
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }
        SimBaseData base = ctx.getSimBaseData();
        List<Integer> triggered = new ArrayList<>(groups.size());
        List<Integer> deferred = new ArrayList<>(groups.size());
        for (int groupId : groups) {
            String requiredPathName = configService.pathNameOfGroup(groupId);
            if (pathMatches(ctx, requiredPathName)) {
                if (base.triggerGuideGroup(groupId)) {
                    triggered.add(groupId);
                }
            } else if (base.deferGuideGroupForScene(groupId)) {
                deferred.add(groupId);
            }
        }
        if (triggered.isEmpty() && deferred.isEmpty()) {
            return Collections.emptyList();
        }
        ctx.setLastSaveTime(0);
        if (!triggered.isEmpty()) {
            log.info("触发新手引导组 playerId={},condition={},param={},groups={}",
                    ctx.playerId(), condition, param, triggered);
            if (notify) {
                notifyTriggeredGroups(ctx, triggered);
            }
        }
        if (!deferred.isEmpty()) {
            log.info("新手引导条件已满足，等待进入指定场景 playerId={},condition={},param={},groups={}",
                    ctx.playerId(), condition, param, deferred);
        }
        return triggered.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(triggered);
    }

    /**
     * 按模拟经营所有场景等级之和 SimBaseData.allLevel 补扫条件3引导。
     * 用于场景升级、开辟新场景、GM 跨级以及进入大厅时补偿漏触发。
     */
    public List<Integer> triggerSceneTotalLevelReached(SimPlayerContext ctx, int allLevel, boolean notify) {
        if (ctx == null || ctx.getSimBaseData() == null || allLevel <= 0) {
            return Collections.emptyList();
        }
        List<Integer> triggered = new ArrayList<>();
        for (int threshold : configService.paramsAtOrBelow(SimConstant.GuideCondition.SCENE_TOTAL_LEVEL, allLevel)) {
            triggered.addAll(trigger(ctx, SimConstant.GuideCondition.SCENE_TOTAL_LEVEL, threshold, false));
        }
        if (triggered.isEmpty()) {
            return Collections.emptyList();
        }
        if (notify) {
            notifyTriggeredGroups(ctx, triggered);
        }
        log.info("场景累计等级达到条件后触发新手引导 playerId={},allLevel={},groups={}",
                ctx.playerId(), allLevel, triggered);
        return Collections.unmodifiableList(triggered);
    }

    /**
     * 按角色系统玩家等级 Player.level 补扫所有已达到的等级引导。
     * 用于正常升级事件、跨级以及进入大厅时对后台直改数据进行补偿。
     */
    public List<Integer> triggerPlayerLevelReached(SimPlayerContext ctx, int playerLevel, boolean notify) {
        if (ctx == null || ctx.getSimBaseData() == null || playerLevel <= 0) {
            return Collections.emptyList();
        }
        List<Integer> triggered = new ArrayList<>();
        for (int threshold : configService.paramsAtOrBelow(SimConstant.GuideCondition.PLAYER_LEVEL, playerLevel)) {
            triggered.addAll(trigger(ctx, SimConstant.GuideCondition.PLAYER_LEVEL, threshold, false));
        }
        if (triggered.isEmpty()) {
            return Collections.emptyList();
        }
        if (notify) {
            notifyTriggeredGroups(ctx, triggered);
        }
        log.info("玩家等级达到条件后触发新手引导 playerId={},playerLevel={},groups={}",
                ctx.playerId(), playerLevel, triggered);
        return Collections.unmodifiableList(triggered);
    }

    /** 玩家进入指定 PathName 后，激活此前条件已经满足的引导组。 */
    public List<Integer> triggerDeferredForPath(SimPlayerContext ctx, String pathName, boolean notify) {
        if (ctx == null || ctx.getSimBaseData() == null || pathName == null || pathName.isBlank()) {
            return Collections.emptyList();
        }
        SimBaseData base = ctx.getSimBaseData();
        List<Integer> triggered = new ArrayList<>();
        boolean stateChanged = false;
        for (int groupId : new ArrayList<>(base.getScenePendingGuideGroupIds())) {
            if (!configService.containsGroup(groupId) || base.hasCompletedGuideGroup(groupId)) {
                base.discardScenePendingGuideGroup(groupId);
                stateChanged = true;
                continue;
            }
            if (pathName.equals(configService.pathNameOfGroup(groupId))
                    && base.activateScenePendingGuideGroup(groupId)) {
                triggered.add(groupId);
                stateChanged = true;
            }
        }
        if (stateChanged) {
            ctx.setLastSaveTime(0);
        }
        if (triggered.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("进入指定场景后触发等待中的新手引导组 playerId={},pathName={},groups={}",
                ctx.playerId(), pathName, triggered);
        if (notify) {
            notifyTriggeredGroups(ctx, triggered);
        }
        return Collections.unmodifiableList(triggered);
    }

    private boolean pathMatches(SimPlayerContext ctx, String requiredPathName) {
        if (requiredPathName == null || requiredPathName.isBlank()) return true;
        if (SimConstant.GuidePath.SIM_HALL.equals(requiredPathName)) {
            return ctx.getPlayerController() != null && ctx.getPlayerController().getScene() == ctx;
        }
        return false;
    }

    /** 玩家正常完成一个引导步骤。 */
    public ResFinishGuide finish(SimPlayerContext ctx, int guideId) {
        FinishGuideResult result = finishWithTriggers(ctx, guideId);
        notifyTriggeredGroups(ctx, result.triggeredGuideGroupIds());
        return result.response();
    }

    /**
     * 玩家正常完成一个引导步骤，但暂不发送由“引导组结束”触发的新引导通知。
     * 调用方应先发送完成响应，再调用 {@link #notifyTriggeredGroups(SimPlayerContext, List)}。
     */
    public FinishGuideResult finishWithTriggers(SimPlayerContext ctx, int guideId) {
        ResFinishGuide res = new ResFinishGuide(Code.SUCCESS);
        res.guideId = guideId;
        if (ctx == null || ctx.getSimBaseData() == null || !configService.containsGuide(guideId)) {
            res.code = Code.PARAM_ERROR;
            return new FinishGuideResult(res, Collections.emptyList());
        }
        int guideGroupId = configService.groupOfGuide(guideId);
        SimBaseData base = ctx.getSimBaseData();
        if (base.getCompletedGuideIds().contains(guideId)) {
            return new FinishGuideResult(res, Collections.emptyList());
        }
        if (!base.hasTriggeredGuideGroup(guideGroupId)) {
            res.code = Code.FAIL;
            return new FinishGuideResult(res, Collections.emptyList());
        }
        boolean firstCompleted = base.completeGuideId(guideId);
        List<Integer> triggeredGroups = finishGroupIfComplete(ctx, guideGroupId, false);
        ctx.setLastSaveTime(0);
        log.info("完成新手引导步骤 playerId={},groupId={},guideId={}",
                ctx.playerId(), guideGroupId, guideId);
        if (firstCompleted) {
            String playerName = ctx.getPlayerController() == null
                    || ctx.getPlayerController().getPlayer() == null
                    ? "" : ctx.getPlayerController().getPlayer().getNickName();
            guideLogger.playerCompleted(ctx.playerId(), playerName, guideId);
        }
        return new FinishGuideResult(res, triggeredGroups);
    }

    /** 发送已经完成状态计算的引导组触发通知。 */
    public void notifyTriggeredGroups(SimPlayerContext ctx, List<Integer> guideGroupIds) {
        if (ctx == null || ctx.getPlayerController() == null
                || guideGroupIds == null || guideGroupIds.isEmpty()) {
            return;
        }
        NotifyGuideTrigger message = new NotifyGuideTrigger(Code.SUCCESS);
        message.guideGroupIds = guideGroupIds;
        ctx.send(message);
    }

    /** 只返回当前配置仍开启的待进行引导组，避免已关闭的历史触发状态继续下发。 */
    public List<Integer> pendingOpenGuideGroupIds(SimPlayerContext ctx) {
        if (ctx == null || ctx.getSimBaseData() == null) {
            return Collections.emptyList();
        }
        List<Integer> pending = ctx.getSimBaseData().pendingGuideGroupIds().stream()
                .filter(configService::containsGroup)
                .toList();
        return pending.isEmpty() ? Collections.emptyList() : pending;
    }

    /**
     * GM 强制完成指定引导步骤。先校验全部 ID，再统一修改，避免部分成功。
     */
    public int forceFinishGuides(SimPlayerContext ctx, Collection<Integer> guideIds) {
        if (ctx == null || ctx.getSimBaseData() == null) {
            return Code.FAIL;
        }
        if (guideIds == null || guideIds.isEmpty()) {
            return Code.PARAM_ERROR;
        }
        Set<Integer> uniqueIds = new LinkedHashSet<>(guideIds);
        for (Integer guideId : uniqueIds) {
            if (guideId == null || !configService.containsGuide(guideId)) return Code.PARAM_ERROR;
        }
        SimBaseData base = ctx.getSimBaseData();
        for (int guideId : uniqueIds) {
            base.completeGuideId(guideId);
            finishGroupIfComplete(ctx, configService.groupOfGuide(guideId));
        }
        ctx.setLastSaveTime(0);
        log.info("GM强制完成指定引导步骤 playerId={},guideIds={}", ctx.playerId(), uniqueIds);
        return Code.SUCCESS;
    }

    /** GM 强制完成配置中的全部引导。 */
    public int forceFinishAll(SimPlayerContext ctx) {
        List<Integer> allGuideIds = configService.allGuideIds();
        int code = forceFinishGuides(ctx, allGuideIds);
        if (code == Code.SUCCESS) {
            log.info("GM强制完成全部引导 playerId={},guideCount={}", ctx.playerId(), allGuideIds.size());
        }
        return code;
    }

    /**
     * 玩家进入大厅或断线重连时，跳过无法恢复表现的当前步骤。
     * 只从组内第一个未完成步骤开始连续跳过，不能越过正常引导步骤。
     */
    public List<Integer> skipReconnectGuides(SimPlayerContext ctx) {
        if (ctx == null || ctx.getSimBaseData() == null) {
            return Collections.emptyList();
        }
        SimBaseData base = ctx.getSimBaseData();
        List<Integer> skipped = new ArrayList<>();
        for (int groupId : base.pendingGuideGroupIds()) {
            List<Integer> guideIds = configService.guideIdsOfGroup(groupId);
            Set<Integer> skipGuideIds = configService.skipGuideIdsOfGroup(groupId);
            if (guideIds.isEmpty() || skipGuideIds.isEmpty()) continue;
            for (int guideId : guideIds) {
                if (base.getCompletedGuideIds().contains(guideId)) continue;
                if (!skipGuideIds.contains(guideId)) break;
                if (base.completeGuideId(guideId)) {
                    skipped.add(guideId);
                    finishGroupIfComplete(ctx, groupId);
                }
            }
        }
        if (!skipped.isEmpty()) {
            ctx.setLastSaveTime(0);
            log.info("玩家进入大厅自动跳过不可恢复引导步骤 playerId={},guideIds={}",
                    ctx.playerId(), skipped);
        }
        return Collections.unmodifiableList(skipped);
    }

    public void triggerItemNotEnough(SimPlayerContext ctx, int itemId) {
        trigger(ctx, SimConstant.GuideCondition.ITEM_NOT_ENOUGH, itemId, true);
    }

    public void triggerItemsAdded(SimPlayerContext ctx, Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Long> entry : items.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                trigger(ctx, SimConstant.GuideCondition.ITEM_GAINED, entry.getKey(), true);
            }
        }
    }

    /** 按当前已经开放的功能ID触发条件7，重复检查由引导组状态自动去重。 */
    public List<Integer> triggerFunctionsUnlocked(SimPlayerContext ctx, Collection<Integer> functionIds, boolean notify) {
        if (ctx == null || functionIds == null || functionIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> triggered = new LinkedHashSet<>();
        for (Integer functionId : functionIds) {
            if (functionId != null && functionId > 0) {
                triggered.addAll(trigger(ctx, SimConstant.GuideCondition.FUNCTION_UNLOCKED, functionId, false));
            }
        }
        if (triggered.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>(triggered);
        if (notify) {
            notifyTriggeredGroups(ctx, result);
        }
        log.info("功能解锁后触发新手引导 playerId={},functionIds={},groups={}",
                ctx.playerId(), functionIds, result);
        return Collections.unmodifiableList(result);
    }

    @Override
    public void onItemsAdded(long playerId, Map<Integer, Long> items, AddType addType) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx != null) {
            triggerItemsAdded(ctx, items);
            return;
        }
        simPackService.forwardPackItemsAdded(playerId, items, addType);
    }

    @Override
    public void onItemsNotEnough(long playerId, Set<Integer> itemIds, AddType addType) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx == null || itemIds == null || itemIds.isEmpty()) {
            return;
        }
        for (int itemId : itemIds) {
            triggerItemNotEnough(ctx, itemId);
        }
    }

    private List<Integer> finishGroupIfComplete(SimPlayerContext ctx, int guideGroupId) {
        return finishGroupIfComplete(ctx, guideGroupId, true);
    }

    private List<Integer> finishGroupIfComplete(SimPlayerContext ctx, int guideGroupId, boolean notify) {
        SimBaseData base = ctx.getSimBaseData();
        List<Integer> groupGuideIds = configService.guideIdsOfGroup(guideGroupId);
        if (groupGuideIds.isEmpty()) return Collections.emptyList();
        // 同一组允许存在多条入口分支，不能要求所有分支步骤都完成。
        // Guide.xlsx 当前约定组内最大 GuideId 为最终结束步骤。
        int finishGuideId = groupGuideIds.get(groupGuideIds.size() - 1);
        if (base.getCompletedGuideIds().contains(finishGuideId)) {
            return complete(ctx, guideGroupId, notify);
        }
        return Collections.emptyList();
    }

    private List<Integer> complete(SimPlayerContext ctx, int guideGroupId, boolean notify) {
        SimBaseData base = ctx.getSimBaseData();
        boolean firstCompleted = base.completeGuideGroup(guideGroupId);
        // 兼容旧逻辑：完成“创建新号”引导组后，开放游客生成和建筑产出。
        if (configService.conditionOfGroup(guideGroupId) == SimConstant.GuideCondition.NEW_PLAYER) {
            base.setGuide(true);
        }
        if (firstCompleted) {
            return trigger(ctx, SimConstant.GuideCondition.GUIDE_GROUP_FINISHED, guideGroupId, notify);
        }
        return Collections.emptyList();
    }

    /** 完成引导响应，以及本次完成后新触发但尚未通知的引导组。 */
    public record FinishGuideResult(ResFinishGuide response, List<Integer> triggeredGuideGroupIds) {
    }
}
