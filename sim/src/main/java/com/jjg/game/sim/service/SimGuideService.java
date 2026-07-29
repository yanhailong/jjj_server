package com.jjg.game.sim.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.listener.ItemAddListener;
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
public class SimGuideService implements ItemAddListener {
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
        List<Integer> triggered = new ArrayList<>(groups.size());
        for (int groupId : groups) {
            if (ctx.getSimBaseData().triggerGuideGroup(groupId)) {
                triggered.add(groupId);
            }
        }
        if (triggered.isEmpty()) {
            return Collections.emptyList();
        }
        ctx.setLastSaveTime(0);
        log.info("触发新手引导组 playerId={},condition={},param={},groups={}",
                ctx.playerId(), condition, param, triggered);
        if (notify && ctx.getPlayerController() != null) {
            NotifyGuideTrigger message = new NotifyGuideTrigger(Code.SUCCESS);
            message.guideGroupIds = triggered;
            ctx.send(message);
        }
        return Collections.unmodifiableList(triggered);
    }

    /** 玩家正常完成一个引导步骤。 */
    public ResFinishGuide finish(SimPlayerContext ctx, int guideId) {
        ResFinishGuide res = new ResFinishGuide(Code.SUCCESS);
        res.guideId = guideId;
        if (ctx == null || ctx.getSimBaseData() == null || !configService.containsGuide(guideId)) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        int guideGroupId = configService.groupOfGuide(guideId);
        SimBaseData base = ctx.getSimBaseData();
        if (base.getCompletedGuideIds().contains(guideId)) {
            return res;
        }
        if (!base.hasTriggeredGuideGroup(guideGroupId)) {
            res.code = Code.FAIL;
            return res;
        }
        boolean firstCompleted = base.completeGuideId(guideId);
        finishGroupIfComplete(ctx, guideGroupId);
        ctx.setLastSaveTime(0);
        log.info("完成新手引导步骤 playerId={},groupId={},guideId={}",
                ctx.playerId(), guideGroupId, guideId);
        if (firstCompleted) {
            String playerName = ctx.getPlayerController() == null
                    || ctx.getPlayerController().getPlayer() == null
                    ? "" : ctx.getPlayerController().getPlayer().getNickName();
            guideLogger.playerCompleted(ctx.playerId(), playerName, guideId);
        }
        return res;
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

    @Override
    public void onItemsAdded(long playerId, Map<Integer, Long> items, AddType addType) {
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx != null) {
            triggerItemsAdded(ctx, items);
            return;
        }
        simPackService.forwardPackItemsAdded(playerId, items, addType);
    }

    private void finishGroupIfComplete(SimPlayerContext ctx, int guideGroupId) {
        SimBaseData base = ctx.getSimBaseData();
        List<Integer> groupGuideIds = configService.guideIdsOfGroup(guideGroupId);
        if (groupGuideIds.isEmpty()) return;
        // 同一组允许存在多条入口分支，不能要求所有分支步骤都完成。
        // Guide.xlsx 当前约定组内最大 GuideId 为最终结束步骤。
        int finishGuideId = groupGuideIds.get(groupGuideIds.size() - 1);
        if (base.getCompletedGuideIds().contains(finishGuideId)) {
            complete(ctx, guideGroupId);
        }
    }

    private void complete(SimPlayerContext ctx, int guideGroupId) {
        SimBaseData base = ctx.getSimBaseData();
        boolean firstCompleted = base.completeGuideGroup(guideGroupId);
        // 兼容旧逻辑：完成“创建新号”引导组后，开放游客生成和建筑产出。
        if (configService.conditionOfGroup(guideGroupId) == SimConstant.GuideCondition.NEW_PLAYER) {
            base.setGuide(true);
        }
        if (firstCompleted) {
            trigger(ctx, SimConstant.GuideCondition.GUIDE_GROUP_FINISHED, guideGroupId, true);
        }
    }
}
