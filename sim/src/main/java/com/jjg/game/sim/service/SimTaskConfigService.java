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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * sim 主线与成就任务配置索引。
 * <p>
 * 主线(taskType=2)继续按 id 升序组成单链；成就(taskType=7)全部独立生效，通过
 * {@link TaskCfg#getBadgeID()} 归属 {@link MedalBuffCfg#getMedalType()}。游戏专属徽章再由
 * MedalBuff.buildID -> BuildingAreaTable.UnlockGameId 得到游戏归属。
 */
@Component
public class SimTaskConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimTaskConfigService.class);

    private volatile List<Integer> mainChain = List.of();
    private volatile Map<Integer, Integer> nextIndex = Map.of();
    private volatile Map<Integer, TaskConditionDef> conditions = Map.of();
    private volatile List<Integer> achievementTaskIds = List.of();
    private volatile Map<Integer, List<Integer>> achievementTasksByBadge = Map.of();
    private volatile Map<Integer, List<Integer>> achievementTasksByGame = Map.of();
    private volatile Map<Class<? extends ConditionEvent>, List<Integer>> achievementTasksByEvent = Map.of();
    private volatile List<Integer> achievementStateTaskIds = List.of();
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
        List<Integer> achievements = new ArrayList<>();
        Map<Integer, List<Integer>> tasksByBadge = new HashMap<>();
        Map<Integer, List<Integer>> tasksByGame = new HashMap<>();
        Map<Class<? extends ConditionEvent>, List<Integer>> tasksByEvent = new HashMap<>();
        List<Integer> stateTasks = new ArrayList<>();
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

            achievements.add(cfg.getId());
            tasksByBadge.computeIfAbsent(cfg.getBadgeID(), ignored -> new ArrayList<>()).add(cfg.getId());
            if (badge.gameId() > 0) {
                tasksByGame.computeIfAbsent(badge.gameId(), ignored -> new ArrayList<>()).add(cfg.getId());
            }
            tasksByEvent.computeIfAbsent(prepared.eventType(), ignored -> new ArrayList<>()).add(cfg.getId());
            if (prepared.eventType() == StateConditionEvent.class) {
                stateTasks.add(cfg.getId());
            }
        }

        mains.sort(Comparator.comparingInt(TaskCfg::getId));
        Map<Integer, Integer> tmpNext = new HashMap<>();
        List<Integer> tmpMain = new ArrayList<>(mains.size());
        for (int i = 0; i < mains.size(); i++) {
            int id = mains.get(i).getId();
            tmpMain.add(id);
            tmpNext.put(id, i + 1 < mains.size() ? mains.get(i + 1).getId() : 0);
        }
        achievements.sort(Integer::compareTo);
        stateTasks.sort(Integer::compareTo);

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
        this.achievementTaskIds = List.copyOf(achievements);
        this.achievementTasksByBadge = immutableListMap(tasksByBadge);
        this.achievementTasksByGame = immutableListMap(tasksByGame);
        this.achievementTasksByEvent = immutableEventMap(tasksByEvent);
        this.achievementStateTaskIds = List.copyOf(stateTasks);
        this.badgeDefinitions = Map.copyOf(tmpBadges);
        log.info("加载 sim 任务配置: 主线 {} 条, 成就 {} 条, 徽章 {} 个, 主线节点={}",
                tmpMain.size(), achievements.size(), tmpBadges.size(), tmpMain);
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

    private static Map<Integer, List<Integer>> immutableListMap(Map<Integer, List<Integer>> source) {
        Map<Integer, List<Integer>> result = new HashMap<>(source.size());
        source.forEach((key, value) -> {
            value.sort(Integer::compareTo);
            result.put(key, List.copyOf(value));
        });
        return Map.copyOf(result);
    }

    private static Map<Class<? extends ConditionEvent>, List<Integer>> immutableEventMap(
            Map<Class<? extends ConditionEvent>, List<Integer>> source) {
        Map<Class<? extends ConditionEvent>, List<Integer>> result = new HashMap<>(source.size());
        source.forEach((key, value) -> {
            value.sort(Integer::compareTo);
            result.put(key, List.copyOf(value));
        });
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

    public List<Integer> achievementTaskIds() {
        return achievementTaskIds;
    }

    public List<Integer> achievementTaskIds(int badgeId) {
        return badgeId <= 0 ? achievementTaskIds
                : achievementTasksByBadge.getOrDefault(badgeId, List.of());
    }

    public List<Integer> achievementStateTaskIds() {
        return achievementStateTaskIds;
    }

    /**
     * 高频游戏事件只检查所属游戏的成就；其他事件按条件事件类型取候选。
     */
    public List<Integer> achievementTaskIdsFor(ConditionEvent event) {
        if (event instanceof GameConditionEvent gameEvent) {
            LinkedHashSet<Integer> ids = new LinkedHashSet<>();
            ids.addAll(achievementTasksByGame.getOrDefault(gameEvent.gameId(), List.of()));
            ids.addAll(achievementTasksByGame.getOrDefault(gameEvent.gameType(), List.of()));
            return ids.isEmpty() ? List.of() : List.copyOf(ids);
        }
        if (event instanceof GameWinEvent gameEvent) {
            return achievementTasksByGame.getOrDefault(gameEvent.gameId(), List.of());
        }
        if (event == null) {
            return List.of();
        }
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        achievementTasksByEvent.forEach((type, taskIds) -> {
            if (type.isInstance(event)) {
                ids.addAll(taskIds);
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
