package com.jjg.game.activity.cashcow.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

/**
 * @author lm
 * @date 2025/9/10 09:57
 */
public class CashCowPlayerActivityData extends PlayerActivityData {

    //总参加次数
    private int joinTimes;

    public CashCowPlayerActivityData() {
    }

    public CashCowPlayerActivityData(long activityId, long round) {
        super(activityId, round);
    }

    public int getJoinTimes() {
        return joinTimes;
    }

    public void setJoinTimes(int joinTimes) {
        this.joinTimes = joinTimes;
    }
}
