package com.jjg.game.core.base.condition.check.record;

import java.math.BigDecimal;

public class PlayerRechargeCondition extends BaseCheckCondition {
    private BigDecimal needAmount;
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