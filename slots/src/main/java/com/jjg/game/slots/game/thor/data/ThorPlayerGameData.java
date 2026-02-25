package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;

/**
 * @author 11
 * @date 2025/12/1 18:01
 */
public class ThorPlayerGameData extends SlotsPlayerGameData {
    private boolean isFreeStart;

    public boolean isFreeStart() {
        return isFreeStart;
    }

    public void setFreeStart(boolean freeStart) {
        isFreeStart = freeStart;
    }
}
