package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.GameRunInfo;

public class WolfMoonGameRunInfo extends GameRunInfo<WolfMoonPlayerGameData> {
    private int freeMultiple;

    public WolfMoonGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public int getFreeMultiple() {
        return freeMultiple;
    }

    public void setFreeMultiple(int freeMultiple) {
        this.freeMultiple = freeMultiple;
    }
}
