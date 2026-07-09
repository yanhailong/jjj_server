package com.jjg.game.season.data;

/**
 * 赛季异步对局结算结果。
 */
public class SeasonMatchResult {
    private String matchId;
    private int result;
    private long playerTotalWin;
    private long opponentTotalWin;
    private long coinChange;
    private long seasonCoin;

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public int getResult() { return result; }
    public void setResult(int result) { this.result = result; }
    public long getPlayerTotalWin() { return playerTotalWin; }
    public void setPlayerTotalWin(long playerTotalWin) { this.playerTotalWin = playerTotalWin; }
    public long getOpponentTotalWin() { return opponentTotalWin; }
    public void setOpponentTotalWin(long opponentTotalWin) { this.opponentTotalWin = opponentTotalWin; }
    public long getCoinChange() { return coinChange; }
    public void setCoinChange(long coinChange) { this.coinChange = coinChange; }
    public long getSeasonCoin() { return seasonCoin; }
    public void setSeasonCoin(long seasonCoin) { this.seasonCoin = seasonCoin; }
}
