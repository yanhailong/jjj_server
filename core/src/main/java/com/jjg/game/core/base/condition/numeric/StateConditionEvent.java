package com.jjg.game.core.base.condition.numeric;

/**
 * 可直接读取的玩家状态。value 是当前状态值，secondaryValue 用于最大次数等第二状态值。
 */
public record StateConditionEvent(Type type, long subjectId, long value,
                                  long secondaryValue) implements ConditionEvent {
    public enum Type {
        PLAYER_LEVEL,
        OPEN_SERVER_DAYS,
        VIP_LEVEL,
        PHONE_BOUND,
        REMAINING_ATTEMPTS
    }
}
