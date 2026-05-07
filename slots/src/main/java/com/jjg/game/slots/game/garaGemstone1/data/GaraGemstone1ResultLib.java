package com.jjg.game.slots.game.garaGemstone1.data;

import com.jjg.game.slots.data.SlotsResultLib;

public class GaraGemstone1ResultLib extends SlotsResultLib<GaraGemstone1AwardLineInfo> {

    private int jackpotId;

    /** 倍数轴中间格子的倍数值，默认1x */
    private long multiplyAxisTimes = 1;


    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public long getMultiplyAxisTimes() {
        return multiplyAxisTimes;
    }

    public void setMultiplyAxisTimes(long multiplyAxisTimes) {
        this.multiplyAxisTimes = multiplyAxisTimes;
    }

}
