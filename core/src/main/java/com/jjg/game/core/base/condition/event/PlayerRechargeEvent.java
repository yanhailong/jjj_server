package com.jjg.game.core.base.condition.event;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/1/14 15:22
 */
public class PlayerRechargeEvent {
    /**
     * 渠道id
     */
    private int channelId;
    /**
     * 充值金额
     */
    private BigDecimal amount;


    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
