package com.jjg.game.core.base.condition.check.record;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2025/10/17 10:56
 */
public abstract class BaseCheckCondition {
    /**
     * 条件达到次数
     */
    private int achievedTimes;
    /**
     * 条件达到最低值
     */
    private BigDecimal minAchievedValue;

    public int getAchievedTimes() {
        return achievedTimes;
    }

    public void setAchievedTimes(int achievedTimes) {
        this.achievedTimes = achievedTimes;
    }

    public BigDecimal getMinAchievedValue() {
        return minAchievedValue;
    }

    public void setMinAchievedValue(BigDecimal minAchievedValue) {
        this.minAchievedValue = minAchievedValue;
    }
}
