package com.jjg.game.slots.game.wealthgod.data;

import com.jjg.game.slots.data.AwardLineInfo;

/**
 * 财神中奖信息
 */
public class WealthGodAwardLineInfo extends AwardLineInfo {

    /**
     * 图标数量
     */
    private int sameCount;

    /**
     * 线id
     */
    private int lineId;

    /**
     * 倍数
     */
    private int baseTimes;

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }

    public int getSameCount() {
        return sameCount;
    }

    public void setSameCount(int sameCount) {
        this.sameCount = sameCount;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    @Override
    public String toString() {
        return
                "[sameCount=" + sameCount +
                        ", lineId=" + lineId +
                        ']';
    }
}
