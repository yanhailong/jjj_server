package com.jjg.game.activity.levelpack.message.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

/**
 * @author lm
 * @date 2025/9/17 16:37
 */
public class PlayerLevelPackData extends PlayerActivityData {
    //触发时间
    private long targetTime;
    //结束时间
    private long buyEndTime;

    public PlayerLevelPackData() {
    }

    public PlayerLevelPackData(long activityId, long round) {
        super(activityId, round);
    }

    public long getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(long targetTime) {
        this.targetTime = targetTime;
    }

    public long getBuyEndTime() {
        return buyEndTime;
    }

    public void setBuyEndTime(long buyEndTime) {
        this.buyEndTime = buyEndTime;
    }
}
