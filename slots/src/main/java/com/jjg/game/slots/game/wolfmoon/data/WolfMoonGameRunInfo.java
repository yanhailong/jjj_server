package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.GameRunInfo;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
public class WolfMoonGameRunInfo extends GameRunInfo<WolfMoonPlayerGameData> {
    private int currentMultiplier;

    public int getCurrentMultiplier() {
        return currentMultiplier;
    }

    public void setCurrentMultiplier(int currentMultiplier) {
        this.currentMultiplier = currentMultiplier;
    }

    public WolfMoonGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

}
