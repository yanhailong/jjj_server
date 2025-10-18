package com.jjg.game.core.base.condition.check.record;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2025/10/17 09:48
 */
public class PlayerRechargeParam extends BaseCheckParam {
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
