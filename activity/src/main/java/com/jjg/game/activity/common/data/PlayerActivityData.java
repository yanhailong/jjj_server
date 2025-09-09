package com.jjg.game.activity.common.data;

/**
 * @author lm
 * @date 2025/9/5 11:49
 */
public class PlayerActivityData {
    //活动id
    private long activityId;
    //领取状态
    private int claimStatus;

    public PlayerActivityData() {
    }

    public PlayerActivityData(long activityId) {
        this.activityId = activityId;
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
