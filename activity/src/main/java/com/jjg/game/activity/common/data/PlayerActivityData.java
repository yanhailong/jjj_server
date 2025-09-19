package com.jjg.game.activity.common.data;

import com.jjg.game.activity.constant.ActivityConstant;

/**
 * @author lm
 * @date 2025/9/5 11:49
 */
public class PlayerActivityData {
    //活动id
    private long activityId;
    //领取状态
    private int claimStatus = ActivityConstant.ClaimStatus.NOT_CLAIM;
    //参加的期数
    private long round;

    public PlayerActivityData() {
    }

    public PlayerActivityData(long activityId, long round) {
        this.activityId = activityId;
        this.round = round;
    }

    public long getRound() {
        return round;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public int getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(int claimStatus) {
        this.claimStatus = claimStatus;
    }
}
