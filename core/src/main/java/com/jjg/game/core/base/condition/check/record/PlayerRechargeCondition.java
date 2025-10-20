package com.jjg.game.core.base.condition.check.record;

import java.math.BigDecimal;

public class PlayerRechargeCondition extends BaseCheckCondition {
    /**
     * 需要充值的金额
     */
    private BigDecimal needAmount;
    /**
     * 渠道id
     */
    private int channelId;

    public BigDecimal getNeedAmount() {
        return needAmount;
    }

    public void setNeedAmount(BigDecimal needAmount) {
        this.needAmount = needAmount;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
}