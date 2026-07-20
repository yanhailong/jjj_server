package com.jjg.game.season.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 上赛季结算快照: 切季时生成, 玩家跨季后首次请求赛季信息时下发并清除。
 */
public class SeasonSettlement {
    private int seasonId;
    //阶段: 1新手, 2进阶, 3循环 (与 SeasonInfo.phase 一致)
    private int phase;
    private int cycleIndex;
    private int rank;
    private int tierId;
    private long totalEarnedCoin;
    //新手试炼累计最高星数 (各关卡最高星求和; 非新手赛季为 0)
    private int totalTrialStars;
    //结算奖励 (含赛季币; 赛季币部分不进邮件, 作为新赛季初始币带入)
    private Map<Integer, Long> rewards = new HashMap<>();
    private long initialCoin;
    //本赛季获得的段位徽章 (勋章id, 取最终段位的 SeasonBadge; 0 表示无)
    private int seasonBadge;

    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }
    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }
    public int getCycleIndex() { return cycleIndex; }
    public void setCycleIndex(int cycleIndex) { this.cycleIndex = cycleIndex; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public int getTierId() { return tierId; }
    public void setTierId(int tierId) { this.tierId = tierId; }
    public long getTotalEarnedCoin() { return totalEarnedCoin; }
    public void setTotalEarnedCoin(long totalEarnedCoin) { this.totalEarnedCoin = totalEarnedCoin; }
    public int getTotalTrialStars() { return totalTrialStars; }
    public void setTotalTrialStars(int totalTrialStars) { this.totalTrialStars = totalTrialStars; }
    public Map<Integer, Long> getRewards() {
        if (rewards == null) rewards = new HashMap<>();
        return rewards;
    }
    public void setRewards(Map<Integer, Long> rewards) { this.rewards = rewards == null ? new HashMap<>() : rewards; }
    public long getInitialCoin() { return initialCoin; }
    public void setInitialCoin(long initialCoin) { this.initialCoin = initialCoin; }
    public int getSeasonBadge() { return seasonBadge; }
    public void setSeasonBadge(int seasonBadge) { this.seasonBadge = seasonBadge; }
}
