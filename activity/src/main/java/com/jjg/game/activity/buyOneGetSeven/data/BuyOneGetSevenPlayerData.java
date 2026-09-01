package com.jjg.game.activity.buyOneGetSeven.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

public class BuyOneGetSevenPlayerData extends PlayerActivityData {
    private long unlockTime;

    public BuyOneGetSevenPlayerData() {
    }

    public BuyOneGetSevenPlayerData(long activityId, long round) {
        super(activityId, round);
    }

    public long getUnlockTime() {
        return unlockTime;
    }

    public void setUnlockTime(long unlockTime) {
        this.unlockTime = unlockTime;
    }
}
