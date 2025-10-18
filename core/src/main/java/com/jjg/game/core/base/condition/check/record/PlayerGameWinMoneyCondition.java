package com.jjg.game.core.base.condition.check.record;

/**
 * @author lm
 * @date 2025/10/17 14:31
 */
public class PlayerGameWinMoneyCondition extends PlayerSampleCondition {
    private long needBet;

    public long getNeedBet() {
        return needBet;
    }

    public void setNeedBet(long needBet) {
        this.needBet = needBet;
    }
}
