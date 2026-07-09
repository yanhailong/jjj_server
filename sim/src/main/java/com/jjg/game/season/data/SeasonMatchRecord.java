package com.jjg.game.season.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 已完成的赛季异步对局记录。
 */
public class SeasonMatchRecord {
    private String matchId;
    private long opponentId;
    private int gameType;
    private long stake;
    private List<Long> playerSpinWins = new ArrayList<>();
    private List<Long> opponentSpinWins = new ArrayList<>();
    private int result;
    private long coinChange;
    private long finishTime;

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

    public List<Long> getPlayerSpinWins() {
        if (playerSpinWins == null) {
            playerSpinWins = new ArrayList<>();
        }
        return playerSpinWins;
    }

    public void setPlayerSpinWins(List<Long> playerSpinWins) {
        this.playerSpinWins = playerSpinWins == null ? new ArrayList<>() : new ArrayList<>(playerSpinWins);
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

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public long getCoinChange() {
        return coinChange;
    }

    public void setCoinChange(long coinChange) {
        this.coinChange = coinChange;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }
}
