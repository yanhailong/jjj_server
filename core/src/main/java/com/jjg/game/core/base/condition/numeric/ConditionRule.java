package com.jjg.game.core.base.condition.numeric;

/**
 * 单个 condition id 的扩展点。
 * <p>
 * 实现必须无状态、线程安全且不得执行 IO；同一进程内同一个 id 只能注册一个实现。
 */
public interface ConditionRule<E extends ConditionEvent> {

    int id();

    Class<E> eventType();

    /** 在配置加载阶段校验参数数量和值域。 */
    void validate(ConditionSpec spec);

    /** 获取该配置的达标目标。 */
    long target(ConditionSpec spec);

    /** 对单次事件进行纯计算。 */
    ConditionUpdate evaluate(ConditionSpec spec, E event);
}
