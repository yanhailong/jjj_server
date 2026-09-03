package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.base.condition.numeric.GameWinEvent;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.ConditionCfg;
import com.jjg.game.sampledata.bean.MedalBuffCfg;
import com.jjg.game.sampledata.bean.TaskCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * sim 主线与成就任务配置索引。
 * <p>
 * 主线(taskType=2)按 id 升序组成单链；成就(taskType=7)按 group 分组后按 id 升序续接，通过
 * {@link TaskCfg#getBadgeID()} 归属 {@link MedalBuffCfg#getMedalType()}。游戏专属徽章再由
 * MedalBuff.buildID -> BuildingAreaTable.UnlockGameId 得到游戏归属。
 */
@Component
public class SimTaskConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimTaskConfigService.class);

    private volatile List<Integer> mainChain = List.of();
    private volatile Map<Integer, Integer> nextIndex = Map.of();
    private volatile Map<Integer, TaskConditionDef> conditions = Map.of();
    private volatile Map<Integer, AchievementGroupDef> achievementGroups = Map.of();
    private volatile List<Integer> globalAchievementGroups = List.of();
    private volatile Map<Integer, List<Integer>> achievementGroupsByBadge = Map.of();
    private volatile Map<Integer, List<Integer>> achievementGroupsByGame = Map.of();
    private volatile Map<Class<? extends ConditionEvent>, List<Integer>> achievementGroupsByEvent = Map.of();
    private volatile Map<Integer, AchievementBadgeDef> badgeDefinitions = Map.of();

    private final ConditionRuleRegistry conditionRules;

    private boolean init = false;

    public SimTaskConfigService() {
        this(ConditionRuleRegistry.standard());
    }

    @Autowired
    public SimTaskConfigService(ConditionRuleRegistry conditionRules) {
        this.conditionRules = conditionRules;
    }

    public void init() {
        init = true;
        loadChains();
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadChains)
                .addChangeSampleFileObserveWithCallBack(ConditionCfg.EXCEL_NAME, this::loadChains)
                .addChangeSampleFileObserveWithCallBack(MedalBuffCfg.EXCEL_NAME, this::loadChains)
                .addChangeSampleFileObserveWithCallBack(BuildingAreaTableCfg.EXCEL_NAME, this::loadChains);
    }

    /**
     * 初始化与热更共用：一次构建不可变索引，玩家热路径只读。
     */
    public void loadChains() {
        if(!init){
            return;
        }
        List<TaskCfg> all = GameDataManager.getTaskCfgList();
        if (all == null || all.isEmpty()) {
            log.warn("加载 sim 任务配置失败: task 配置为空");
            return;
        }

        Map<Integer, BadgeSeed> badgeSeeds = loadBadgeSeeds();
        List<TaskCfg> mains = new ArrayList<>();
        Map<Integer, List<TaskCfg>> groupedAchievements = new HashMap<>();
        Map<Integer, List<Integer>> tasksByBadge = new HashMap<>();
        Map<Integer, TaskConditionDef> tmpConditions = new HashMap<>();

        for (TaskCfg cfg : all) {
            if (cfg == null || (cfg.getTaskType() != TaskConstant.TaskType.MAIN_LINE
                    && cfg.getTaskType() != TaskConstant.TaskType.ACHIEVEMENT)) {
                continue;
            }
            BadgeSeed badge = null;
            if (cfg.getTaskType() == TaskConstant.TaskType.ACHIEVEMENT) {
                badge = badgeSeeds.get(cfg.getBadgeID());
                if (cfg.getBadgeID() <= 0 || badge == null) {
                    log.warn("成就任务缺少有效徽章配置, 不加载 taskId={},badgeId={}",
                            cfg.getId(), cfg.getBadgeID());
                    continue;
                }
            }

            PreparedCondition prepared;
            try {
                prepared = conditionRules.prepare(ConditionSpec.from(cfg.getTaskConditionId()));
            } catch (IllegalArgumentException e) {
                log.warn("sim 任务条件配置非法, 不加载 taskId={},condition={},error={}",
                        cfg.getId(), cfg.getTaskConditionId(), e.getMessage());
                continue;
            }
            if (prepared.eventType() == StateConditionEvent.class
                    && !SimTaskStateEventFactory.supports(prepared)) {
                log.warn("sim 任务状态条件缺少可靠数据源, 不加载 taskId={},conditionId={}",
                        cfg.getId(), prepared.spec().id());
                continue;
            }
            if (badge != null && badge.gameId() <= 0
                    && GameWinEvent.class.isAssignableFrom(prepared.eventType())) {
                log.warn("全局徽章任务不能由游戏结算推进, 不加载 taskId={},badgeId={},conditionId={}",
                        cfg.getId(), cfg.getBadgeID(), prepared.spec().id());
                continue;
            }

            ConditionCfg conditionCfg = GameDataManager.getConditionCfg(prepared.spec().id());
            String legacyType = conditionCfg == null ? null : conditionCfg.getTriggerEventType();
            String counterType = legacyType == null || legacyType.isBlank()
                    ? "condition" + prepared.spec().id() : legacyType;
            tmpConditions.put(cfg.getId(), new TaskConditionDef(prepared, counterType));

            if (cfg.getTaskType() == TaskConstant.TaskType.MAIN_LINE) {
                mains.add(cfg);
                continue;
            }

            groupedAchievements.computeIfAbsent(cfg.getGroup(), ignored -> new ArrayList<>()).add(cfg);
        }

        mains.sort(Comparator.comparingInt(TaskCfg::getId));
        Map<Integer, Integer> tmpNext = new HashMap<>();
        List<Integer> tmpMain = new ArrayList<>(mains.size());
        for (int i = 0; i < mains.size(); i++) {
            int id = mains.get(i).getId();
            tmpMain.add(id);
            tmpNext.put(id, i + 1 < mains.size() ? mains.get(i + 1).getId() : 0);
        }
        Map<Integer, AchievementGroupDef> tmpGroups = new HashMap<>();
        List<Integer> globalGroups = new ArrayList<>();
        Map<Integer, Set<Integer>> groupsByBadge = new HashMap<>();
        Map<Integer, Set<Integer>> groupsByGame = new HashMap<>();
        Map<Class<? extends ConditionEvent>, Set<Integer>> groupsByEvent = new HashMap<>();
        for (Map.Entry<Integer, List<TaskCfg>> entry : groupedAchievements.entrySet()) {
            List<TaskCfg> chain = entry.getValue();
            chain.sort(Comparator.comparingInt(TaskCfg::getId));
            TaskCfg first = chain.getFirst();
            if ((first.getQuality() != 0 && first.getQuality() != 1)
                    || chain.stream().anyMatch(cfg -> cfg.getQuality() != first.getQuality()
                    || cfg.getBadgeID() != first.getBadgeID())) {
                log.warn("成就任务组的 Quality 或 BadgeID 配置不一致或无效, 不加载 group={}", entry.getKey());
                chain.forEach(cfg -> tmpConditions.remove(cfg.getId()));
                continue;
            }
            int groupId = entry.getKey();
            List<Integer> ids = chain.stream().map(TaskCfg::getId).toList();
            tmpGroups.put(groupId, new AchievementGroupDef(groupId, first.getBadgeID(), first.getQuality(), ids));
            if (first.getQuality() == 0) {
                globalGroups.add(groupId);
            }
            groupsByBadge.computeIfAbsent(first.getBadgeID(), ignored -> new LinkedHashSet<>()).add(groupId);
            tasksByBadge.computeIfAbsent(first.getBadgeID(), ignored -> new ArrayList<>()).addAll(ids);
            int gameId = badgeSeeds.get(first.getBadgeID()).gameId();
            if (gameId > 0) {
                groupsByGame.computeIfAbsent(gameId, ignored -> new LinkedHashSet<>()).add(groupId);
            }
            for (int i = 0; i < ids.size(); i++) {
                int taskId = ids.get(i);
                tmpNext.put(taskId, i + 1 < ids.size() ? ids.get(i + 1) : 0);
                groupsByEvent.computeIfAbsent(tmpConditions.get(taskId).condition().eventType(),
                        ignored -> new LinkedHashSet<>()).add(groupId);
            }
        }
        globalGroups.sort(Integer::compareTo);

        Map<Integer, AchievementBadgeDef> tmpBadges = new HashMap<>();
        for (BadgeSeed seed : badgeSeeds.values()) {
            List<Integer> taskIds = tasksByBadge.getOrDefault(seed.badgeId(), List.of());
            if (taskIds.isEmpty()) {
                continue;
            }
            tmpBadges.put(seed.badgeId(), new AchievementBadgeDef(
                    seed.badgeId(), seed.buildingId(), seed.gameId(), List.copyOf(taskIds), seed.tiers()));
        }

        this.mainChain = List.copyOf(tmpMain);
        this.nextIndex = Map.copyOf(tmpNext);
        this.conditions = Map.copyOf(tmpConditions);
        this.achievementGroups = Map.copyOf(tmpGroups);
        this.globalAchievementGroups = List.copyOf(globalGroups);
        this.achievementGroupsByBadge = immutableGroupMap(groupsByBadge);
        this.achievementGroupsByGame = immutableGroupMap(groupsByGame);
        this.achievementGroupsByEvent = immutableGroupMap(groupsByEvent);
        this.badgeDefinitions = Map.copyOf(tmpBadges);
        log.info("加载 sim 任务配置: 主线 {} 条, 成就 {} 组, 徽章 {} 个, 主线节点={}",
                tmpMain.size(), tmpGroups.size(), tmpBadges.size(), tmpMain);
    }

    private Map<Integer, BadgeSeed> loadBadgeSeeds() {
        List<MedalBuffCfg> all = GameDataManager.getMedalBuffCfgList();
        if (all == null || all.isEmpty()) {
            log.warn("加载成就徽章配置失败: MedalBuff 配置为空");
            return Map.of();
        }
        Map<Integer, List<MedalBuffCfg>> grouped = new HashMap<>();
        for (MedalBuffCfg cfg : all) {
            if (cfg != null && cfg.getMedalType() > 0) {
                grouped.computeIfAbsent(cfg.getMedalType(), ignored -> new ArrayList<>()).add(cfg);
            }
        }

        Map<Integer, BadgeSeed> result = new HashMap<>();
        for (Map.Entry<Integer, List<MedalBuffCfg>> entry : grouped.entrySet()) {
            List<MedalBuffCfg> configs = entry.getValue();
            configs.sort(Comparator.comparingInt(MedalBuffCfg::getCollectNum)
                    .thenComparingInt(MedalBuffCfg::getId));
            int buildingId = configs.getFirst().getBuildID();
            int gameId = 0;
            if (buildingId > 0) {
                BuildingAreaTableCfg building = GameDataManager.getBuildingAreaTableCfg(buildingId);
                if (building == null || building.getUnlockGameId() <= 0) {
                    log.warn("徽章缺少有效游戏建筑配置, 不加载 badgeId={},buildingId={}",
                            entry.getKey(), buildingId);
                    continue;
                }
                gameId = building.getUnlockGameId();
            }

            List<BadgeBuffTier> tiers = new ArrayList<>(configs.size());
            for (MedalBuffCfg cfg : configs) {
                if (cfg.getBuildID() != buildingId) {
                    log.warn("同一徽章档位配置的建筑不一致, 忽略 cfgId={},badgeId={},buildingId={},expected={}",
                            cfg.getId(), entry.getKey(), cfg.getBuildID(), buildingId);
                    continue;
                }
                EnumMap<BuildingOutputType, Integer> buffs = new EnumMap<>(BuildingOutputType.class);
                if (cfg.getBuffId() != null) {
                    for (Map.Entry<Integer, Integer> buff : cfg.getBuffId().entrySet()) {
                        BuildingOutputType type = buff.getKey() == null
                                ? null : BuildingOutputType.fromCode(buff.getKey());
                        if (type == null) {
                            log.warn("徽章加成类型无效, 忽略 cfgId={},outputType={}", cfg.getId(), buff.getKey());
                        } else if (buff.getValue() != null && buff.getValue() != 0) {
                            buffs.merge(type, buff.getValue(), Integer::sum);
                        }
                    }
                }
                tiers.add(new BadgeBuffTier(cfg.getId(), cfg.getCollectNum(),
                        Collections.unmodifiableMap(buffs)));
            }
            if (!tiers.isEmpty()) {
                result.put(entry.getKey(), new BadgeSeed(
                        entry.getKey(), buildingId, gameId, List.copyOf(tiers)));
            }
        }
        return result;
    }

    private static <K> Map<K, List<Integer>> immutableGroupMap(Map<K, Set<Integer>> source) {
        Map<K, List<Integer>> result = new HashMap<>(source.size());
        source.forEach((key, value) -> result.put(key, value.stream().sorted().toList()));
        return Map.copyOf(result);
    }

    public int firstMain() {
        List<Integer> chain = this.mainChain;
        return chain.isEmpty() ? 0 : chain.getFirst();
    }

    public int next(int taskId) {
        return this.nextIndex.getOrDefault(taskId, 0);
    }

    public TaskConditionDef conditionOf(int taskId) {
        return conditions.get(taskId);
    }

    public Collection<AchievementGroupDef> achievementGroups() {
        return achievementGroups.values();
    }

    public AchievementGroupDef achievementGroup(int groupId) {
        return achievementGroups.get(groupId);
    }

    public List<Integer> globalAchievementGroups() {
        return globalAchievementGroups;
    }

    public List<Integer> achievementGroups(int badgeId) {
        return achievementGroupsByBadge.getOrDefault(badgeId, List.of());
    }

    /**
     * 高频游戏事件只检查所属游戏的成就；其他事件按条件事件类型取候选。
     */
    public List<Integer> achievementGroupsFor(ConditionEvent event) {
        if (event instanceof GameConditionEvent gameEvent) {
            List<Integer> gameGroups = achievementGroupsByGame.getOrDefault(gameEvent.gameId(), List.of());
            if (gameEvent.gameId() == gameEvent.gameType()) {
                return gameGroups;
            }
            LinkedHashSet<Integer> ids = new LinkedHashSet<>();
            ids.addAll(gameGroups);
            ids.addAll(achievementGroupsByGame.getOrDefault(gameEvent.gameType(), List.of()));
            return ids.isEmpty() ? List.of() : List.copyOf(ids);
        }
        if (event instanceof GameWinEvent gameEvent) {
            return achievementGroupsByGame.getOrDefault(gameEvent.gameId(), List.of());
        }
        if (event == null) {
            return List.of();
        }
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        achievementGroupsByEvent.forEach((type, groupIds) -> {
            if (type.isInstance(event)) {
                ids.addAll(groupIds);
            }
        });
        return ids.isEmpty() ? List.of() : List.copyOf(ids);
    }

    public AchievementBadgeDef badge(int badgeId) {
        return badgeDefinitions.get(badgeId);
    }

    public List<AchievementBadgeDef> badges() {
        return badgeDefinitions.values().stream()
                .sorted(Comparator.comparingInt(AchievementBadgeDef::badgeId))
                .toList();
    }

    /**
     * 配置加载后不可变，可被玩家热路径无锁复用。
     */
    public record TaskConditionDef(PreparedCondition condition, String counterType) {
    }

    public record AchievementGroupDef(int groupId, int badgeId, int quality, List<Integer> taskIds) {
        /** 当前节点之前的任务均已领取，无需为玩家保存历史节点。 */
        public int precedingTaskCount(int taskId) {
            return Collections.binarySearch(taskIds, taskId);
        }
    }

    public record BadgeBuffTier(int cfgId, int collectNum, Map<BuildingOutputType, Integer> buffs) {
    }

    public record AchievementBadgeDef(int badgeId, int buildingId, int gameId,
                                      List<Integer> taskIds, List<BadgeBuffTier> tiers) {
        public BadgeBuffTier activeTier(int completedCount) {
            BadgeBuffTier active = null;
            for (BadgeBuffTier tier : tiers) {
                if (tier.collectNum() > completedCount) {
                    break;
                }
                active = tier;
            }
            return active;
        }

        public BadgeBuffTier nextTier(int completedCount) {
            for (BadgeBuffTier tier : tiers) {
                if (tier.collectNum() > completedCount) {
                    return tier;
                }
            }
            return null;
        }
    }

    private record BadgeSeed(int badgeId, int buildingId, int gameId, List<BadgeBuffTier> tiers) {
    }

    public List<Integer> getMainChain() {
        return mainChain;
    }
}
