package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/2/25 14:54
 */
@Document
@CompoundIndexes({@CompoundIndex(name = "idx_player_recharge_time", def = "{'playerId': 1,'channelId':1,'rechargeTime': 1}"),
        @CompoundIndex(name = "idx_player_recharge_time_all_channel", def = "{'playerId': 1,'rechargeTime': 1}")
})
public class PlayerRechargeFlow {
    //订单id
    @Indexed(unique = true, sparse = true)
    private String orderId;
    //玩家id
    private long playerId;
    //充值时间
    private long rechargeTime;
    //充值金额
    private BigDecimal amount;
    //渠道id
    private int channelId;

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getRechargeTime() {
        return rechargeTime;
    }

    public void setRechargeTime(long rechargeTime) {
        this.rechargeTime = rechargeTime;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
