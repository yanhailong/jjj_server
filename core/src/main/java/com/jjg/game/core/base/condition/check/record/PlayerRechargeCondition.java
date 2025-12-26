package com.jjg.game.core.base.condition.check.record;

public class PlayerRechargeCondition extends BaseCheckCondition {
    /**
     * 渠道id
     */
    private int channelId;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
}