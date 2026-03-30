package com.jjg.game.slots.game.cleopatra.data;

/**
 * @author 11
 * @date 2025/9/11 11:26
 */
public class AddColumnConfig {
    //中奖次数
    private int winCount;
    //滚轴id
    private int rollerId;
    //倍率
    private int times;

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getRollerId() {
        return rollerId;
    }

    public void setRollerId(int rollerId) {
        this.rollerId = rollerId;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }
}
