package com.jjg.game.core.data;

/**
 * @author lm
 * @date 2025/12/24 15:08
 */
public class RankEntry {
    private long playerId;
    private long points;
    private long rank;

    public RankEntry(long playerId, long points, long rank) {
        this.playerId = playerId;
        this.points = points;
        this.rank = rank;
    }

    public long getPlayerId() {
        return playerId;
    }

    public long getPoints() {
        return points;
    }

    public long getRank() {
        return rank;
    }
}

