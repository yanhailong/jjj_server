package com.jjg.game.core.base.condition.check.record;

/**
 * @author lm
 * @date 2025/10/17 10:56
 */
public abstract class BaseCheckCondition {
    private int achievedTimes;
    private long minAchievedValue;

    public int getAchievedTimes() {
        return achievedTimes;
    }

    public void setAchievedTimes(int achievedTimes) {
        this.achievedTimes = achievedTimes;
    }

    public long getMinAchievedValue() {
        return minAchievedValue;
    }

    public void setMinAchievedValue(long minAchievedValue) {
        this.minAchievedValue = minAchievedValue;
    }
}
