package com.jjg.game.hall.levelpack.data;

/**
 * @author lm
 * @date 2025/9/17 16:37
 */
public class PlayerLevelPackData {
    //配置id
    private int id;
    //领取状态
    private int claimStatus;
    //触发时间
    private long targetTime;
    //结束时间
    private long buyEndTime;

    public PlayerLevelPackData() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(int claimStatus) {
        this.claimStatus = claimStatus;
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
