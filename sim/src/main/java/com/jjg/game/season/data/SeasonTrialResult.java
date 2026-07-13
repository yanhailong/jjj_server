package com.jjg.game.season.data;

import java.util.Map;

/**
 * 一次试炼挑战/被动判定的结算结果 (含本次新发放的星级奖励)。
 */
public class SeasonTrialResult {
    private int trialId;
    //本次达成星级 (0=失败)
    private int achievedStars;
    //历史最高星级 (结算后)
    private int bestStars;
    //本次新发放的奖励 (仅新突破的星级)
    private Map<Integer, Long> rewards;
    private int spinCount;
    private long progress;
    private long seasonCoin;

    public int getTrialId() {
        return trialId;
    }

    public void setTrialId(int trialId) {
        this.trialId = trialId;
    }

    public int getAchievedStars() {
        return achievedStars;
    }

    public void setAchievedStars(int achievedStars) {
        this.achievedStars = achievedStars;
    }

    public int getBestStars() {
        return bestStars;
    }

    public void setBestStars(int bestStars) {
        this.bestStars = bestStars;
    }

    public Map<Integer, Long> getRewards() {
        return rewards;
    }

    public void setRewards(Map<Integer, Long> rewards) {
        this.rewards = rewards;
    }

    public int getSpinCount() {
        return spinCount;
    }

    public void setSpinCount(int spinCount) {
        this.spinCount = spinCount;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getSeasonCoin() {
        return seasonCoin;
    }

    public void setSeasonCoin(long seasonCoin) {
        this.seasonCoin = seasonCoin;
    }
}
