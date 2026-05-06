package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.core.constant.GameConstant;

/**
 * 空袭游戏单个注单
 * 每个玩家最多持有2个注单(betIndex 0/1)
 *
 * @author 11
 * @date 2026/3/27
 */
public class AirRaidBetData {
    //投注金额
    private long betAmount;
    //是否已兑现
    private boolean cashedOut;
    //兑现时的倍率(百分比, 100 = 1.00x)
    private int cashOutMultiplier;
    //赢得金额
    private long winAmount;

    public AirRaidBetData(long betAmount) {
        this.betAmount = betAmount;
    }

    /**
     * 兑现：标记已兑现，计算赢得金额
     * winAmount = betAmount × multiplier / 10000
     *
     * @param multiplier 当前倍率(万分比, 10000 = 1.00x)
     */
    public void cashOut(int multiplier) {
        this.cashedOut = true;
        this.cashOutMultiplier = multiplier;
        this.winAmount = betAmount * multiplier / GameConstant.TEN_THOUSAND;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long betAmount) {
        this.betAmount = betAmount;
    }

    public boolean isCashedOut() {
        return cashedOut;
    }

    public void setCashedOut(boolean cashedOut) {
        this.cashedOut = cashedOut;
    }

    public int getCashOutMultiplier() {
        return cashOutMultiplier;
    }

    public void setCashOutMultiplier(int cashOutMultiplier) {
        this.cashOutMultiplier = cashOutMultiplier;
    }

    public long getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(long winAmount) {
        this.winAmount = winAmount;
    }
}
