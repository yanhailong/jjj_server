package com.jjg.game.core.task.param;

import com.jjg.game.core.base.condition.numeric.RechargeConditionEvent;

/**
 * 充值任务事件参数。
 * <p>
 * {@code addValue} 使用统一的最小货币单位；{@code channelId} 是支付渠道，
 * 与充值流水 {@code PlayerRechargeFlow.channelId} 的口径保持一致。
 */
public class TaskConditionParamRecharge extends DefaultTaskConditionParam {
    private int channelId;
    private RechargeConditionEvent conditionEvent;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
        conditionEvent = null;
    }

    @Override
    public void setAddValue(long addValue) {
        super.setAddValue(addValue);
        conditionEvent = null;
    }

    /** 每次业务触发只构造一个不可变事实事件，供同条件下的多个任务配置复用。 */
    public RechargeConditionEvent conditionEvent() {
        if (conditionEvent == null) {
            conditionEvent = new RechargeConditionEvent(channelId, addValue);
        }
        return conditionEvent;
    }
}
