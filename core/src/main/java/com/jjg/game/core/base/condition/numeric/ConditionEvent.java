package com.jjg.game.core.base.condition.numeric;

/**
 * 统一条件事件标记接口。
 * <p>
 * core 只定义稳定的事件数据契约，不负责切线程、跨节点投递或持久化。调用方应在现有玩家
 * worker/房间串行上下文中同步求值；新模块可以实现自己的事件和 {@link ConditionRule} 扩展。
 */
public interface ConditionEvent {
}
