package com.jjg.game.core.base.condition.numeric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数值条件规则注册表。构造完成后为不可变 Map，求值阶段只有一次哈希查找，不加锁。
 */
@Component
public class ConditionRuleRegistry {
    private final Map<Integer, ConditionRule<?>> rules;

    @Autowired
    public ConditionRuleRegistry(List<ConditionRule<?>> extensions) {
        this(DefaultConditionRules.rules(), extensions);
    }

    private ConditionRuleRegistry(List<ConditionRule<?>> builtIns, List<ConditionRule<?>> extensions) {
        Map<Integer, ConditionRule<?>> indexed = new LinkedHashMap<>();
        builtIns.forEach(rule -> register(indexed, rule));
        extensions.forEach(rule -> register(indexed, rule));
        this.rules = Map.copyOf(indexed);
    }

    /** 供不启动 Spring 的纯单元测试和轻量工具使用。 */
    public static ConditionRuleRegistry standard() {
        return StandardHolder.INSTANCE;
    }

    public boolean supports(int conditionId) {
        return rules.containsKey(conditionId);
    }

    public PreparedCondition prepare(ConditionSpec spec) {
        spec = canonicalize(spec);
        ConditionRule<?> rule = rules.get(spec.id());
        if (rule == null) {
            throw new IllegalArgumentException("unsupported condition id: " + spec.id());
        }
        return new PreparedCondition(spec, rule);
    }

    /**
     * 仓库内旧 task.xlsx 曾使用 11002_金额_0。标准格式是 11002_渠道_金额；由于目标不允许为 0，
     * 该历史形式不存在歧义，在统一入口转换后所有下游只处理标准顺序。
     */
    private static ConditionSpec canonicalize(ConditionSpec spec) {
        if (spec.id() == 11002 && spec.parameters().size() == 2
                && spec.parameter(0) > 0 && spec.parameter(1) == 0) {
            return new ConditionSpec(11002, List.of(0L, spec.parameter(0)));
        }
        return spec;
    }

    private static void register(Map<Integer, ConditionRule<?>> indexed, ConditionRule<?> rule) {
        if (rule == null) {
            throw new IllegalStateException("condition rule must not be null");
        }
        ConditionRule<?> previous = indexed.putIfAbsent(rule.id(), rule);
        if (previous != null) {
            throw new IllegalStateException("duplicate condition rule id " + rule.id()
                    + ": " + previous.getClass().getName() + " and " + rule.getClass().getName());
        }
    }

    private static final class StandardHolder {
        private static final ConditionRuleRegistry INSTANCE =
                new ConditionRuleRegistry(DefaultConditionRules.rules(), List.of());
    }
}
