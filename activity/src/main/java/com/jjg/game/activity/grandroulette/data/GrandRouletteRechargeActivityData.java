package com.jjg.game.activity.grandroulette.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

/**
 * 大转盘活动数据
 *
 * @author 11
 * @date 2026/3/5
 */
public class GrandRouletteRechargeActivityData extends PlayerActivityData {
    /**
     * 唯一标识
     */
    private long id;
    /**
     * 结束时间
     */
    private long endTime;
    /**
     * 累计金币
     */
    private long cumulativeGold;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getCumulativeGold() {
        return cumulativeGold;
    }

    public void setCumulativeGold(long cumulativeGold) {
        this.cumulativeGold = cumulativeGold;
    }
}
