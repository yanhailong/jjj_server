package com.jjg.game.alliance.data;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/29
 */
public class DonateCfg {
    //捐献的物品和数量
    private int itemId;
    //用户每日捐献的次数
    private List<Long> counts;
    //奖励的贡献值
    private int rewardContribution;
    //奖励的声誉值
    private int rewardReputation;
    //用户每日捐献的次数
    private int memberDailyLimit;

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public List<Long> getCounts() {
        return counts;
    }

    public void setCounts(List<Long> counts) {
        this.counts = counts;
    }

    public int getRewardContribution() {
        return rewardContribution;
    }

    public void setRewardContribution(int rewardContribution) {
        this.rewardContribution = rewardContribution;
    }

    public int getRewardReputation() {
        return rewardReputation;
    }

    public void setRewardReputation(int rewardReputation) {
        this.rewardReputation = rewardReputation;
    }

    public int getMemberDailyLimit() {
        return memberDailyLimit;
    }

    public void setMemberDailyLimit(int memberDailyLimit) {
        this.memberDailyLimit = memberDailyLimit;
    }
}
