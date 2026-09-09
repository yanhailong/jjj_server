package com.jjg.game.sim.listener;

import com.jjg.game.core.base.condition.numeric.ConditionEvent;
import com.jjg.game.sim.data.SimPlayerContext;

/**
 * 模拟经营事实事件监听器。生产者只投递事实，各功能独立维护自己的进度和持久化。
 */
public interface SimConditionEventListener {
    /** 返回本次产生的后续事实事件；没有后续事件时返回 null。 */
    ConditionEvent onConditionEvent(SimPlayerContext ctx, ConditionEvent event);
}
