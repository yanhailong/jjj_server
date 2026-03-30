package com.jjg.game.slots.data;

/**
 * 下注划分
 */
public class BetDivideInfo {
    //流入标准池
    private long toBigPool;
    //流入标准池
    private long toSmallPool;
    //收益
    private long inCome;
    //税收
    private long tax;

    public long getToBigPool() {
        return toBigPool;
    }

    public void setToBigPool(long toBigPool) {
        this.toBigPool = toBigPool;
    }

    public long getToSmallPool() {
        return toSmallPool;
    }

    public void setToSmallPool(long toSmallPool) {
        this.toSmallPool = toSmallPool;
    }

    public long getInCome() {
        return inCome;
    }

    public void setInCome(long inCome) {
        this.inCome = inCome;
    }

    public long getTax() {
        return tax;
    }

    public void setTax(long tax) {
        this.tax = tax;
    }
}
