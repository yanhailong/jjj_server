package com.jjg.game.room.data.room;

/**
 * 结算数据
 *
 * @author 2CL
 */
public class SettlementData {
    // 玩家压分的净赢值
    private long betWin;
    // 赢的总值
    private long totalWin;
    // 压的总值 必须自己设置值
    private long betTotal;
    // 税收
    private long taxation;
    //庄家实际赢的值
    private long bankerWind;

    public SettlementData() {
    }

    public SettlementData(long betWin, long totalWin, long betTotal, long taxation) {
        this.betWin = betWin;
        this.totalWin = totalWin;
        this.betTotal = betTotal;
        this.taxation = taxation;
        if (betWin > 0) {
            this.bankerWind += betTotal;
        }
    }

    public long getBetWin() {
        return betWin;
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
        this.totalWin += settlementData.totalWin;
        this.taxation += settlementData.taxation;
        this.betTotal += settlementData.betTotal;
        this.bankerWind += settlementData.bankerWind;
        return this;
    }

    public long getBankerWind() {
        return bankerWind;
    }

    /**
     * 获取没扣税之前的金额
     *
     * @return 没扣税之前的金额
     */
    public long getTotalGet() {
        return totalWin + taxation;
    }
}
