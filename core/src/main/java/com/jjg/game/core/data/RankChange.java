package com.jjg.game.core.data;

/**
 * @author lm
 * @date 2025/12/24 15:09
 */
public class RankChange {
    private long playerId;
    private int addPoints;

    public RankChange(long playerId, int addPoints) {
        this.playerId = playerId;
        this.addPoints = addPoints;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public void setAddPoints(int addPoints) {
        this.addPoints = addPoints;
    }

    public long getPlayerId() {
        return playerId;
    }

    public int getAddPoints() {
        return addPoints;
    }
}
