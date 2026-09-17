package com.jjg.game.sim.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.SimConstant;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** 配置容器更换时重建索引；玩家事件不重复解析条件或扫描整张任务表。 */
@Service
public class ActivePassConfigService {
    private static final Logger log = LoggerFactory.getLogger(ActivePassConfigService.class);
    private final ConditionRuleRegistry rules;
    private volatile Index cached;

    public ActivePassConfigService(ConditionRuleRegistry rules) { this.rules = rules; }

    public Index index() {
        Object passes = GameDataManager.getInstance().getCfgContainer(ActivePassCfg.class);
        Object rewards = GameDataManager.getInstance().getCfgContainer(PassRewardCfg.class);
        Object tasks = GameDataManager.getInstance().getCfgContainer(TaskCfg.class);
        Object conditions = GameDataManager.getInstance().getCfgContainer(ConditionCfg.class);
        Index value = cached;
        if (value == null || value.passSource != passes || value.rewardSource != rewards
                || value.taskSource != tasks || value.conditionSource != conditions) {
            value = new Index(passes, rewards, tasks, conditions, GameDataManager.getActivePassCfgList(),
                    GameDataManager.getPassRewardCfgList(), GameDataManager.getTaskCfgList(), rules);
            cached = value;
        }
        return value;
    }

    public PurchasePrice purchasePrice() {
        GlobalConfigCfg limit = GameDataManager.getGlobalConfigCfg(SimConstant.Global.PURCHASE_LIMIT);
        GlobalConfigCfg price = GameDataManager.getGlobalConfigCfg(SimConstant.Global.PURCHASE_PRICE);
        if (limit == null || limit.getIntValue() <= 0 || price == null || price.getValue() == null) {
            return null;
        }
        String[] parts = price.getValue().split("_");
        if (parts.length != 3) { return null; }
        try {
            int itemId = Integer.parseInt(parts[0].trim());
            long count = Long.parseLong(parts[1].trim());
            int points = Integer.parseInt(parts[2].trim());
            return itemId > 0 && count > 0 && points > 0 && GameDataManager.getItemCfg(itemId) != null
                    ? new PurchasePrice(itemId, count, points, limit.getIntValue()) : null;
        } catch (NumberFormatException e) { return null; }
    }

    public record PurchasePrice(int itemId, long count, int points, int limit) { }
    public record Period(ActivePassCfg config, long start, long end, List<PassRewardCfg> rewards) {
        public int id() { return config.getId(); }
        public boolean active(long now) { return config.getIsOpen() && now >= start && now < end; }
        public int level(long points) {
            int result = 0;
            for (PassRewardCfg reward : rewards) {
                if (points < reward.getActivePoints()) { break; }
                result = reward.getLevel();
            }
            return result;
        }
    }
    public record TaskDefinition(TaskCfg config, PreparedCondition condition) { }

    public static final class Index {
        private final Object passSource, rewardSource, taskSource, conditionSource;
        private final Map<Integer, Period> periods = new LinkedHashMap<>();
        private final List<Period> openPeriods;
        private final Map<Integer, TaskDefinition> tasks = new HashMap<>();
        private final Map<Integer, List<TaskDefinition>> daily = new TreeMap<>();
        private final Map<Integer, List<TaskDefinition>> periodic = new TreeMap<>();

        Index(Object passSource, Object rewardSource, Object taskSource, Object conditionSource,
              List<ActivePassCfg> passes, List<PassRewardCfg> rewards, List<TaskCfg> taskConfigs,
              ConditionRuleRegistry rules) {
            this.passSource = passSource;
            this.rewardSource = rewardSource;
            this.taskSource = taskSource;
            this.conditionSource = conditionSource;
            Map<Integer, List<PassRewardCfg>> groups = new HashMap<>();
            for (PassRewardCfg reward : rewards) {
                groups.computeIfAbsent(reward.getRewardGroup(), key -> new ArrayList<>()).add(reward);
            }
            for (ActivePassCfg pass : passes) {
                try {
                    List<PassRewardCfg> levels = groups.getOrDefault(pass.getRewardGroup(), List.of()).stream()
                            .sorted(Comparator.comparingInt(PassRewardCfg::getLevel)).toList();
                    int previousLevel = 0;
                    int previousPoints = -1;
                    for (PassRewardCfg level : levels) {
                        if (level.getLevel() <= previousLevel || level.getActivePoints() <= previousPoints) {
                            throw new IllegalArgumentException("活跃通行证等级或累计积分非递增 passId=" + pass.getId());
                        }
                        previousLevel = level.getLevel();
                        previousPoints = level.getActivePoints();
                    }
                    long start = TimeHelper.getTimestamp(pass.getTime_start());
                    long end = TimeHelper.getTimestamp(pass.getTime_end());
                    if (start <= 0 || start >= end || levels.isEmpty()) {
                        throw new IllegalArgumentException("活跃通行证时间或奖励为空 passId=" + pass.getId());
                    }
                    periods.put(pass.getId(), new Period(pass, start, end, levels));
                } catch (IllegalArgumentException e) {
                    log.error("活跃通行证配置无效，停用该期 passId={}", pass.getId(), e);
                }
            }
            openPeriods = periods.values().stream().filter(period -> period.config().getIsOpen())
                    .sorted(Comparator.comparingLong(Period::start)).toList();
            for (TaskCfg cfg : taskConfigs) {
                Map<Integer, List<TaskDefinition>> target = switch (cfg.getTaskType()) {
                    case TaskConstant.TaskType.ACTIVE_PASS_DAILY -> daily;
                    case TaskConstant.TaskType.ACTIVE_PASS_PERIOD -> periodic;
                    default -> null;
                };
                if (target == null) { continue; }
                try {
                    if (cfg.getIntegralNum() < 0) { throw new IllegalArgumentException("任务积分不能为负数"); }
                    TaskDefinition task = new TaskDefinition(cfg, rules.prepare(ConditionSpec.from(cfg.getTaskConditionId())));
                    tasks.put(cfg.getId(), task);
                    target.computeIfAbsent(cfg.getGroup(), key -> new ArrayList<>()).add(task);
                } catch (IllegalArgumentException e) {
                    log.error("活跃通行证任务配置无效，不加载 taskId={}", cfg.getId(), e);
                }
            }
            daily.values().forEach(list -> list.sort(Comparator.comparingInt(t -> t.config().getId())));
            periodic.values().forEach(list -> list.sort(Comparator.comparingInt(t -> t.config().getId())));
        }
        public Period active(long now) {
            int low = 0;
            int high = openPeriods.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (openPeriods.get(middle).start() <= now) { low = middle + 1; }
                else { high = middle; }
            }
            Period period = low == 0 ? null : openPeriods.get(low - 1);
            return period != null && period.active(now) ? period : null;
        }
        public Period period(int id) { return periods.get(id); }
        public TaskDefinition task(int id) { return tasks.get(id); }
        public Map<Integer, List<TaskDefinition>> daily() { return daily; }
        public Map<Integer, List<TaskDefinition>> periodic() { return periodic; }
        public TaskDefinition next(int group, int id) {
            for (TaskDefinition task : periodic.getOrDefault(group, List.of())) {
                if (task.config().getId() > id) { return task; }
            }
            return null;
        }
    }
}
