package com.jjg.game.slots.game.captainjack.data;

import com.jjg.game.slots.data.GameRunInfo;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class CaptainJackGameRunInfo extends GameRunInfo<CaptainJackPlayerGameData> {

    public CaptainJackGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //剩余探宝次数
    private int remainDigCount;
    //本次探宝倍率
    private int digTimesMultiplier;
    //免费游戏累计获得金币
    private long freeAmount;

    private long mini;
    private long minor;
    private long major;
    private long grand;

    public long getFreeAmount() {
        return freeAmount;
    }

    public void setFreeAmount(long freeAmount) {
        this.freeAmount = freeAmount;
    }

    public int getRemainDigCount() {
        return remainDigCount;
    }

    public void setRemainDigCount(int remainDigCount) {
        this.remainDigCount = remainDigCount;
    }

    public int getDigTimesMultiplier() {
        return digTimesMultiplier;
    }

    public void setDigTimesMultiplier(int digTimesMultiplier) {
        this.digTimesMultiplier = digTimesMultiplier;
    }

    public long getMini() {
        return mini;
    }

    public void setMini(long mini) {
        this.mini = mini;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getGrand() {
        return grand;
    }

    public void setGrand(long grand) {
        this.grand = grand;
    }
}
