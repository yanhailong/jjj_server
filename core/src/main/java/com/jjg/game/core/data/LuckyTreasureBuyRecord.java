package com.jjg.game.core.data;

/**
 * 夺宝奇兵购买记录
 */
public class LuckyTreasureBuyRecord {

    /**
     * 购买时间
     */
    private long buyTime;

    /**
     * 购买的玩家id
     */
    private long playerId;

    /**
     * 购买的数量
     */
    private long buyCount;

    public long getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(long buyTime) {
        this.buyTime = buyTime;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(long buyCount) {
        this.buyCount = buyCount;
    }
}
