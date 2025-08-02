package com.jjg.game.room.data.room;

/**
 * @author lm
 * @date 2025/7/26 11:23
 */
public class PokerPlayerGameData {
    /**
     * 加入房间的时间
     */
    private long joinTime;

    /**
     * 临时货币
     */
    private long tempCurrency;

    /**
     * 是否初始化完成
     */
    private boolean isInit;


    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public long getTempCurrency() {
        return tempCurrency;
    }

    public void setTempCurrency(long tempCurrency) {
        this.tempCurrency = tempCurrency;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }
}
