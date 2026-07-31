package com.jjg.game.season.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家当前进行中的异步赛季对局。
 */
public class SeasonMatchSession {
    private String matchId;
    private long opponentId;
    //匹配时从候选文档快照的对手展示信息, 结算落记录直接使用, 免二次查库
    private String opponentName;
    private int opponentHeadImgId;
    private int opponentHeadFrameId;
    private int opponentTierId;
    private int gameType;
    private long stake;
    /** 主动匹配是否已托管 stake；被动匹配只验资，不预扣。 */
    private boolean stakeEscrowed;
    /** 触发被动匹配的已完成旋转，不计入对局局数；主动匹配为 0。 */
    private long excludedSpinId;
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

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public int getOpponentHeadImgId() {
        return opponentHeadImgId;
    }

    public void setOpponentHeadImgId(int opponentHeadImgId) {
        this.opponentHeadImgId = opponentHeadImgId;
    }

    public int getOpponentHeadFrameId() {
        return opponentHeadFrameId;
    }

    public void setOpponentHeadFrameId(int opponentHeadFrameId) {
        this.opponentHeadFrameId = opponentHeadFrameId;
    }

    public int getOpponentTierId() {
        return opponentTierId;
    }

    public void setOpponentTierId(int opponentTierId) {
        this.opponentTierId = opponentTierId;
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

    public boolean isStakeEscrowed() {
        return stakeEscrowed;
    }

    public void setStakeEscrowed(boolean stakeEscrowed) {
        this.stakeEscrowed = stakeEscrowed;
    }

    public long getExcludedSpinId() {
        return excludedSpinId;
    }

    public void setExcludedSpinId(long excludedSpinId) {
        this.excludedSpinId = excludedSpinId;
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
