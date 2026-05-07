package com.jjg.game.slots.game.garaGemstone3.data;

import com.jjg.game.slots.data.BaseLineAwardLineInfo;

public class GaraGemstone3AwardLineInfo extends BaseLineAwardLineInfo {

    /** 多格图标分裂倍数，默认1；若同一中奖线有多个分裂图标则累乘（如2x和3x → splitTimes=6） */
    private int splitTimes = 1;
    /** 该中奖线最终倍数 = baseTimes * splitTimes */
    private int totalTimes;

    public int getSplitTimes() {
        return splitTimes;
    }

    public void setSplitTimes(int splitTimes) {
        this.splitTimes = splitTimes;
    }

    public int getTotalTimes() {
        return totalTimes;
    }

    public void setTotalTimes(int totalTimes) {
        this.totalTimes = totalTimes;
    }
}
