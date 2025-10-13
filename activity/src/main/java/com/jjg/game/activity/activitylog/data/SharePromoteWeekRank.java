package com.jjg.game.activity.activitylog.data;

import java.util.Map;

/**
 * @author lm
 * @date 2025/10/13 14:12
 */
public class SharePromoteWeekRank {
    /**
     * 玩家id
     */
    private long playerId;
    /**
     * 玩家名
     */
    private String name;
    /**
     * 总分数
     */
    private long totalScore;
    /**
     * 排名
     */
    private int rank;
    /**
     * 奖励
     */
    private Map<Integer, Long> rewards;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(long totalScore) {
        this.totalScore = totalScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Map<Integer, Long> getRewards() {
        return rewards;
    }

    public void setRewards(Map<Integer, Long> rewards) {
        this.rewards = rewards;
    }
}
