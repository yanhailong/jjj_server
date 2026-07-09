package com.jjg.game.sim.season.data;

/**
 * 赛季排行榜条目。
 */
public class SeasonRankEntry {
    private int rank;
    private long playerId;
    private long seasonCoin;
    private long totalEarnedCoin;
    private int tierId;

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public long getSeasonCoin() { return seasonCoin; }
    public void setSeasonCoin(long seasonCoin) { this.seasonCoin = seasonCoin; }
    public long getTotalEarnedCoin() { return totalEarnedCoin; }
    public void setTotalEarnedCoin(long totalEarnedCoin) { this.totalEarnedCoin = totalEarnedCoin; }
    public int getTierId() { return tierId; }
    public void setTierId(int tierId) { this.tierId = tierId; }
}
