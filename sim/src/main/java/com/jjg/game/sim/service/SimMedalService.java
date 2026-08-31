package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.RankChange;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.core.service.RankService;
import com.jjg.game.core.task.db.TaskDetail;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimTaskData;
import com.jjg.game.sim.pb.res.ResChangeShowMedal;
import com.jjg.game.sim.pb.res.ResMedalPanel;
import com.jjg.game.sim.pb.struct.AchievementBadgeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 成就徽章服务：按 BadgeID 汇总完成任务数，解析 MedalBuff 档位，维护排行及固定值加成缓存。
 */
@Service
public class SimMedalService {
    private static final Logger log = LoggerFactory.getLogger(SimMedalService.class);
    private static final String MEDAL_RANK_KEY = "sim:medal:rank";
    private static final int BASIS_POINTS = 10000;

    @Autowired
    private RankService rankService;
    @Autowired
    private SimTaskConfigService taskConfig;

    /** 玩家至少完成一个对应成就后即获得徽章，是否激活 Buff 档位由 CollectNum 独立决定。 */
    public List<Integer> getActivatedMedalIds(SimPlayerContext ctx) {
        SimTaskData data = ctx == null ? null : ctx.getSimTaskData();
        if (data == null) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (SimTaskConfigService.AchievementBadgeDef badge : taskConfig.badges()) {
            int completed = completedCount(data, badge);
            if (completed > 0) {
                result.add(badge.badgeId());
            }
        }
        return result;
    }

    public ResMedalPanel buildMedalPanel(SimPlayerContext ctx) {
        ResMedalPanel res = new ResMedalPanel(Code.SUCCESS);
        SimTaskData data = ctx == null ? null : ctx.getSimTaskData();
        if (data == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }

        List<BadgeSnapshot> snapshots = new ArrayList<>();
        int completedTotal = 0;
        int taskTotal = 0;
        for (SimTaskConfigService.AchievementBadgeDef badge : taskConfig.badges()) {
            int completed = completedCount(data, badge);
            completedTotal += completed;
            taskTotal += badge.taskIds().size();
            snapshots.add(new BadgeSnapshot(
                    badge, completed, badge.activeTier(completed), badge.nextTier(completed)));
        }
        snapshots.sort(Comparator
                .comparing((BadgeSnapshot snapshot) -> !snapshot.unlocked())
                .thenComparing(Comparator.comparingInt(BadgeSnapshot::activeCollectNum).reversed())
                .thenComparingInt(snapshot -> snapshot.badge().badgeId()));

        EnumMap<BuildingOutputType, Integer> activeBuffs = calcActiveMedalBuffs(data);
        ctx.setMedalBuffMap(activeBuffs);
        res.completedAchievementCount = completedTotal;
        res.totalAchievementCount = taskTotal;
        res.rankBasisPoints = syncAndGetRankBasisPoints(ctx.playerId(), completedTotal);
        res.activeBuffs = toKvList(activeBuffs);
        res.badgeInfos = snapshots.stream().map(this::toInfo).toList();
        res.medalShowMax = medalShowMax();
        res.showMedalIds = sanitizeDisplayedBadges(ctx, getActivatedMedalIds(ctx), res.medalShowMax);
        return res;
    }

    /** 修改场景和个人简介共用的徽章展示列表。空列表表示全部取消。 */
    public ResChangeShowMedal changeShowMedal(SimPlayerContext ctx, List<Integer> newMedalIds) {
        ResChangeShowMedal res = new ResChangeShowMedal(Code.SUCCESS);
        SimTaskData data = ctx == null ? null : ctx.getSimTaskData();
        if (data == null) {
            res.code = Code.NOT_FOUND;
            return res;
        }

        List<Integer> requested = newMedalIds == null ? List.of() : newMedalIds;
        int showMax = medalShowMax();
        LinkedHashSet<Integer> unique = new LinkedHashSet<>(requested);
        Set<Integer> activated = new HashSet<>(getActivatedMedalIds(ctx));
        if (requested.size() > showMax || unique.size() != requested.size()
                || unique.contains(null) || !activated.containsAll(unique)) {
            res.code = Code.PARAM_ERROR;
            res.showMedalIds = new ArrayList<>(data.getDisplayedMedalIds());
            return res;
        }

        List<Integer> displayed = new ArrayList<>(requested);
        data.setDisplayedMedalIds(displayed);
        ctx.setLastSaveTime(0);
        res.showMedalIds = new ArrayList<>(displayed);
        log.info("玩家[{}]设置展示徽章 {}", ctx.playerId(), displayed);
        return res;
    }

    /** 成就完成后一次刷新排行分值与固定值加成缓存。 */
    public void refreshAchievementState(SimPlayerContext ctx) {
        if (ctx == null || ctx.getSimTaskData() == null) {
            return;
        }
        try {
            SimTaskData data = ctx.getSimTaskData();
            ctx.setMedalBuffMap(calcActiveMedalBuffs(data));
            syncRankScore(ctx.playerId(), completedAchievementCount(data));
        } catch (Exception e) {
            log.warn("刷新成就徽章状态失败 playerId={}", ctx.playerId(), e);
        }
    }

    /** 登录任务数据加载完成后刷新内存加成缓存。 */
    public void refreshMedalBonusCache(SimPlayerContext ctx) {
        if (ctx == null || ctx.getSimTaskData() == null) {
            return;
        }
        try {
            ctx.setMedalBuffMap(calcActiveMedalBuffs(ctx.getSimTaskData()));
        } catch (Exception e) {
            log.warn("刷新成就徽章加成缓存失败 playerId={}", ctx.playerId(), e);
        }
    }

    /** 每个徽章只取完成数达到的最高档，再合并其 BuffId 固定值。 */
    EnumMap<BuildingOutputType, Integer> calcActiveMedalBuffs(SimTaskData data) {
        EnumMap<BuildingOutputType, Integer> merged = new EnumMap<>(BuildingOutputType.class);
        if (data == null) {
            return merged;
        }
        for (SimTaskConfigService.AchievementBadgeDef badge : taskConfig.badges()) {
            SimTaskConfigService.BadgeBuffTier active = badge.activeTier(completedCount(data, badge));
            if (active == null || active.buffs().isEmpty()) {
                continue;
            }
            active.buffs().forEach((type, value) -> merged.merge(type, value, Integer::sum));
        }
        return merged;
    }

    /** 指定产出类型的徽章固定值；管理属性(11)会叠加到服务/知名度/曝光度。 */
    public int getFixedBonus(SimPlayerContext ctx, BuildingOutputType type) {
        if (ctx == null || type == null || ctx.getMedalBuffMap().isEmpty()) {
            return 0;
        }
        int value = ctx.getMedalBuffMap().getOrDefault(type, 0);
        if (type.bonusGroup() != type) {
            value += ctx.getMedalBuffMap().getOrDefault(type.bonusGroup(), 0);
        }
        return value;
    }

    public int getAdGoldBonusFixed(SimPlayerContext ctx) {
        return getFixedBonus(ctx, BuildingOutputType.WATCH_ADS_ADD_GOLD);
    }

    private int completedAchievementCount(SimTaskData data) {
        int count = 0;
        for (SimTaskConfigService.AchievementBadgeDef badge : taskConfig.badges()) {
            count += completedCount(data, badge);
        }
        return count;
    }

    private int completedCount(SimTaskData data, SimTaskConfigService.AchievementBadgeDef badge) {
        int count = 0;
        for (Integer taskId : badge.taskIds()) {
            TaskDetail task = data.getAchievementTasks().get(taskId);
            if (task != null && task.getStatus() != TaskConstant.TaskStatus.STATUS_IN_PROGRESS) {
                count++;
            }
        }
        return count;
    }

    private AchievementBadgeInfo toInfo(BadgeSnapshot snapshot) {
        AchievementBadgeInfo info = new AchievementBadgeInfo();
        info.badgeId = snapshot.badge().badgeId();
        info.buildingId = snapshot.badge().buildingId();
        info.gameId = snapshot.badge().gameId();
        info.completedTaskCount = snapshot.completedCount();
        info.totalTaskCount = snapshot.badge().taskIds().size();
        info.currentBuffCfgId = snapshot.active() == null ? 0 : snapshot.active().cfgId();
        info.nextBuffCfgId = snapshot.next() == null ? 0 : snapshot.next().cfgId();
        return info;
    }

    private List<KVInfo> toKvList(Map<BuildingOutputType, Integer> buffs) {
        if (buffs == null || buffs.isEmpty()) {
            return List.of();
        }
        return buffs.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getCode()))
                .map(entry -> new KVInfo(entry.getKey().getCode(), entry.getValue()))
                .toList();
    }

    private List<Integer> sanitizeDisplayedBadges(SimPlayerContext ctx, List<Integer> activated, int showMax) {
        Set<Integer> allowed = new HashSet<>(activated);
        List<Integer> displayed = new ArrayList<>();
        for (Integer badgeId : ctx.getSimTaskData().getDisplayedMedalIds()) {
            if (displayed.size() >= showMax) {
                break;
            }
            if (badgeId != null && allowed.contains(badgeId) && !displayed.contains(badgeId)) {
                displayed.add(badgeId);
            }
        }
        if (!displayed.equals(ctx.getSimTaskData().getDisplayedMedalIds())) {
            ctx.getSimTaskData().setDisplayedMedalIds(displayed);
            ctx.setLastSaveTime(0);
        }
        return new ArrayList<>(displayed);
    }

    private int medalShowMax() {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.MEDAL_SHOW_MAX_ID);
        return cfg == null ? 0 : Math.max(0, cfg.getIntValue());
    }

    private int syncAndGetRankBasisPoints(long playerId, int completedCount) {
        syncRankScore(playerId, completedCount);
        if (completedCount <= 0) {
            return 0;
        }
        RankEntry my = rankService.getRank(MEDAL_RANK_KEY, playerId);
        int total = rankService.size(MEDAL_RANK_KEY);
        if (my == null || total <= 0) {
            return 0;
        }
        double ratio = 1.0 - (double) my.getRank() / total;
        int basisPoints = (int) Math.round(ratio * BASIS_POINTS);
        return Math.max(0, Math.min(BASIS_POINTS, basisPoints));
    }

    private void syncRankScore(long playerId, int completedCount) {
        if (completedCount <= 0) {
            return;
        }
        long current = rankService.getPoints(MEDAL_RANK_KEY, playerId);
        if (current != completedCount) {
            rankService.batchSetPoints(MEDAL_RANK_KEY, List.of(new RankChange(playerId, completedCount)));
        }
    }

    private record BadgeSnapshot(SimTaskConfigService.AchievementBadgeDef badge, int completedCount,
                                 SimTaskConfigService.BadgeBuffTier active,
                                 SimTaskConfigService.BadgeBuffTier next) {
        boolean unlocked() {
            return completedCount > 0;
        }

        int activeCollectNum() {
            return active == null ? 0 : active.collectNum();
        }
    }
}
