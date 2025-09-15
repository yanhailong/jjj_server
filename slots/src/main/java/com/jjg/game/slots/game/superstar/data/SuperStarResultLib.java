package com.jjg.game.slots.game.superstar.data;

import com.jjg.game.slots.data.SlotsResultLib;

public class SuperStarResultLib extends SlotsResultLib<SuperStarAwardLineInfo> {

    private int jackpotId;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
