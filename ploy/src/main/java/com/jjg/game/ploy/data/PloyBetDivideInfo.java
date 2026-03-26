package com.jjg.game.ploy.data;

/**
 * @author 11
 * @date 2026/3/19
 */
public class PloyBetDivideInfo {
    //流入标准池
    private long toBigPool;
    //税收
    private long tax;
    //池子变化值
    private long poolChangeValue;
    //池子变化后的值
    private long poolAfterValue;
    //玩家变化前的值
    private long playerBeforeMoney;
    //玩家变化后的值
    private long playerAfterMoney;

    public long getToBigPool() {
        return toBigPool;
    }

    public void setToBigPool(long toBigPool) {
        this.toBigPool = toBigPool;
    }

    public long getTax() {
        return tax;
    }

    public void setTax(long tax) {
        this.tax = tax;
    }

    public long getPoolChangeValue() {
        return poolChangeValue;
    }

    public void setPoolChangeValue(long poolChangeValue) {
        this.poolChangeValue = poolChangeValue;
    }

    public long getPoolAfterValue() {
        return poolAfterValue;
    }

    public void setPoolAfterValue(long poolAfterValue) {
        this.poolAfterValue = poolAfterValue;
    }

    public long getPlayerBeforeMoney() {
        return playerBeforeMoney;
    }

    public void setPlayerBeforeMoney(long playerBeforeMoney) {
        this.playerBeforeMoney = playerBeforeMoney;
    }

    public long getPlayerAfterMoney() {
        return playerAfterMoney;
    }

    public void setPlayerAfterMoney(long playerAfterMoney) {
        this.playerAfterMoney = playerAfterMoney;
    }
}
