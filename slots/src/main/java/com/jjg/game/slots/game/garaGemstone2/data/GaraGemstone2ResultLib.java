package com.jjg.game.slots.game.garaGemstone2.data;

import com.jjg.game.slots.data.SlotsResultLib;

public class GaraGemstone2ResultLib extends SlotsResultLib<GaraGemstone2AwardLineInfo> {

    private int jackpotId;

    /** 倍数轴中间格子的倍数值，默认1x */
    private long multiplyAxisTimes = 1;

    /** wheel模式随机出的单线押分倍数，0表示未触发 */
    private long wheelTimes;

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

    public long getWheelTimes() {
        return wheelTimes;
    }

    public void setWheelTimes(long wheelTimes) {
        this.wheelTimes = wheelTimes;
    }

}
