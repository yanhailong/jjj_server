package com.jjg.game.season.data;

/**
 * 赛季排行榜条目。
 */
public class SeasonRankEntry {
    private int rank;
    private long playerId;
    private String playerName;
    private long seasonCoin;
    private long totalEarnedCoin;
    /** 阶段：1新手，2进阶，3循环 */
    private int phase;

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public long getSeasonCoin() { return seasonCoin; }
    public void setSeasonCoin(long seasonCoin) { this.seasonCoin = seasonCoin; }
    public long getTotalEarnedCoin() { return totalEarnedCoin; }
    public void setTotalEarnedCoin(long totalEarnedCoin) { this.totalEarnedCoin = totalEarnedCoin; }
    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }
}
