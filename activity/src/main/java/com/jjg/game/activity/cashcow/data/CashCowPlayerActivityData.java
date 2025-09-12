package com.jjg.game.activity.cashcow.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

/**
 * @author lm
 * @date 2025/9/10 09:57
 */
public class CashCowPlayerActivityData extends PlayerActivityData {

    //总参加次数
    private int joinTimes;
    //剩余免费次数
    private int remainFreeTimes;

    public CashCowPlayerActivityData() {
    }

    public CashCowPlayerActivityData(long activityId, long round) {
        super(activityId, round);
    }

    public int getRemainFreeTimes() {
        return remainFreeTimes;
    }

    public void setRemainFreeTimes(int remainFreeTimes) {
        this.remainFreeTimes = remainFreeTimes;
    }

    public int getJoinTimes() {
        return joinTimes;
    }

    public void setJoinTimes(int joinTimes) {
        this.joinTimes = joinTimes;
    }
}
