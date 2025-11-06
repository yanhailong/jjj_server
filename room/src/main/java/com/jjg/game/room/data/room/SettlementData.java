package com.jjg.game.room.data.room;

/**
 * 结算数据
 *
 * @author 2CL
 */
public class SettlementData {
    // 玩家压分的净赢值
    private long betWin;
    // 返还压分
    private long betReturn;
    // 赢的总值
    private long totalWin;
    // 压的总值
    private long betTotal;
    // 税收
    private long taxation;

    public SettlementData() {
    }

    public SettlementData(long betWin, long betReturn, long totalWin, long betTotal, long taxation) {
        this.betWin = betWin;
        this.betReturn = betReturn;
        this.totalWin = totalWin;
        this.betTotal = betTotal;
        this.taxation = taxation;
    }

    public long getBetWin() {
        return betWin;
    }

    public long getBetReturn() {
        return betReturn;
    }

    public long getTotalWin() {
        return totalWin;
    }

    public long getBetTotal() {
        return betTotal;
    }

    public void setBetTotal(long betTotal) {
        this.betTotal = betTotal;
    }

    public long getTaxation() {
        return taxation;
    }

    /**
     * 增加结算值
     */
    public SettlementData increaseBySettlementData(SettlementData settlementData) {
        this.betWin += settlementData.betWin;
        this.betReturn += settlementData.betReturn;
        this.totalWin += settlementData.totalWin;
        this.betTotal += settlementData.betTotal;
        this.taxation += settlementData.taxation;
        return this;
    }
}
