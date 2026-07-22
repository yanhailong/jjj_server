package com.jjg.game.core.task.condition;

import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.sampledata.bean.TaskCfg;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 旧 TaskManager 到统一 condition 注册表的配置编译缓存。
 * <p>
 * 旧任务系统没有配置加载期扩展点，因此首次使用时编译；热路径只查询缓存。热更复用同一 task id 时，
 * 会比较完整参数并自动替换，缓存中只保留每个任务 id 的最新版本。
 */
final class TaskConditionCache {
    private final ConditionRuleRegistry conditionRules;
    private final ConcurrentMap<Integer, CachedCondition> conditions = new ConcurrentHashMap<>();

    TaskConditionCache(ConditionRuleRegistry conditionRules) {
        this.conditionRules = conditionRules;
    }

    PreparedCondition get(TaskCfg taskCfg) {
        List<Long> source = taskCfg.getTaskConditionId();
        CachedCondition cached = conditions.get(taskCfg.getId());
        if (cached != null && cached.source().equals(source)) {
            return cached.condition();
        }
        PreparedCondition prepared = conditionRules.prepare(ConditionSpec.from(source));
        conditions.put(taskCfg.getId(), new CachedCondition(List.copyOf(source), prepared));
        return prepared;
    }

    private record CachedCondition(List<Long> source, PreparedCondition condition) {
    }
}
