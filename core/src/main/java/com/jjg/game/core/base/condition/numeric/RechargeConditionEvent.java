package com.jjg.game.core.base.condition.numeric;

/** 一笔已成功入账的充值。amount 使用业务统一的最小货币单位。 */
public record RechargeConditionEvent(int channelId, long amount) implements ConditionEvent {

    public boolean matchesChannel(long expected) {
        return expected <= 0 || expected == channelId;
    }
}
