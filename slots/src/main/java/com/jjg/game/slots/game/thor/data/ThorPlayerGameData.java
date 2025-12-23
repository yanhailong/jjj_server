package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
public class ThorPlayerGameData extends SlotsPlayerGameData {
    //免费模式累计奖励
    private long freeModeTotalReward = 0;

    public long getFreeModeTotalReward() {
        return freeModeTotalReward;
    }

    public void setFreeModeTotalReward(long freeModeTotalReward) {
        this.freeModeTotalReward = freeModeTotalReward;
    }

    public void addFreeModeTotalReward(long value) {
        this.freeModeTotalReward += value;
    }
}
