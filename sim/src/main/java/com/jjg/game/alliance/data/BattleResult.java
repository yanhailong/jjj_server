package com.jjg.game.alliance.data;

/**
 * 单个联盟的对决结算结果 (内嵌于 {@link AllianceBattleData#getResults()})。
 * <p>
 * 镜像协议: 结算只比"我方总分 vs 我对手总分", 各联盟独立判定, 平局按胜利发奖。
 *
 * @author 11
 * @date 2026/6/11
 */
public class BattleResult {
    //对手联盟 id
    private long opponentId;
    //我方总分 (结算时刻快照)
    private long myScore;
    //对手总分 (结算时刻快照)
    private long oppScore;
    //是否获胜 (平局按胜利处理)
    private boolean win;
    //结算时间(ms)
    private long settleTime;

    public BattleResult() {
    }

    public BattleResult(long opponentId, long myScore, long oppScore, boolean win, long settleTime) {
        this.opponentId = opponentId;
        this.myScore = myScore;
        this.oppScore = oppScore;
        this.win = win;
        this.settleTime = settleTime;
    }

    public long getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(long opponentId) {
        this.opponentId = opponentId;
    }

    public long getMyScore() {
        return myScore;
    }

    public void setMyScore(long myScore) {
        this.myScore = myScore;
    }

    public long getOppScore() {
        return oppScore;
    }

    public void setOppScore(long oppScore) {
        this.oppScore = oppScore;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public long getSettleTime() {
        return settleTime;
    }

    public void setSettleTime(long settleTime) {
        this.settleTime = settleTime;
    }
}
