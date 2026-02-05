package com.jjg.game.slots.game.luckymouse.data;

import com.jjg.game.slots.data.SlotsResultLib;

public class LuckyMouseResultLib extends SlotsResultLib<LuckyMouseAwardLineInfo> {

    private int jackpotId;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
