package com.jjg.game.core.base.condition.numeric;

/**
 * 已校验的条件。配置加载时创建一次，热路径可直接 O(1) 求值，避免重复解析和临时对象。
 */
public final class PreparedCondition {
    private final ConditionSpec spec;
    private final ConditionRule<ConditionEvent> rule;
    private final long target;

    @SuppressWarnings("unchecked")
    PreparedCondition(ConditionSpec spec, ConditionRule<?> rule) {
        this.spec = spec;
        this.rule = (ConditionRule<ConditionEvent>) rule;
        this.rule.validate(spec);
        this.target = this.rule.target(spec);
    }

    public ConditionSpec spec() {
        return spec;
    }

    public long target() {
        return target;
    }

    public Class<? extends ConditionEvent> eventType() {
        return rule.eventType();
    }

    /**
     * 事件类型不属于本条件时返回 ignored，便于一个事件依次投递给多个功能而无需异常分支。
     */
    public ConditionUpdate evaluate(ConditionEvent event) {
        if (event == null || !rule.eventType().isInstance(event)) {
            return ConditionUpdate.ignored(target);
        }
        return rule.evaluate(spec, event);
    }
}
