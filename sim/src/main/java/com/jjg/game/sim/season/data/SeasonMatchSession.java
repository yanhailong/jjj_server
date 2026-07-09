package com.jjg.game.sim.season.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家当前进行中的异步赛季对局。
 */
public class SeasonMatchSession {
    private String matchId;
    private long opponentId;
    private int gameType;
    private long stake;
    private int expectedSpins;
    private long startedAt;
    private List<Long> opponentSpinWins = new ArrayList<>();
    private List<Long> playerSpinWins = new ArrayList<>();

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public long getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(long opponentId) {
        this.opponentId = opponentId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public long getStake() {
        return stake;
    }

    public void setStake(long stake) {
        this.stake = stake;
    }

    public int getExpectedSpins() {
        return expectedSpins;
    }

    public void setExpectedSpins(int expectedSpins) {
        this.expectedSpins = expectedSpins;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public List<Long> getOpponentSpinWins() {
        if (opponentSpinWins == null) {
            opponentSpinWins = new ArrayList<>();
        }
        return opponentSpinWins;
    }

    public void setOpponentSpinWins(List<Long> opponentSpinWins) {
        this.opponentSpinWins = opponentSpinWins == null ? new ArrayList<>() : new ArrayList<>(opponentSpinWins);
    }

    public List<Long> getPlayerSpinWins() {
        if (playerSpinWins == null) {
            playerSpinWins = new ArrayList<>();
        }
        return playerSpinWins;
    }

    public void setPlayerSpinWins(List<Long> playerSpinWins) {
        this.playerSpinWins = playerSpinWins == null ? new ArrayList<>() : new ArrayList<>(playerSpinWins);
    }
}
