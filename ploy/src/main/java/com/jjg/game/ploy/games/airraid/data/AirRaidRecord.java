package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.ploy.data.PloyRecord;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2026/3/27
 */
@Document
public class AirRaidRecord extends PloyRecord {
    //投注金额
    private long betAmount;
    //坠毁倍率(万分比)
    private int crashMultiplier;
    //兑现倍率(万分比), 未兑现为0
    private int cashOutMultiplier;
    //赢得金额
    private long winAmount;
    //是否兑现
    private boolean cashedOut;

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long betAmount) {
        this.betAmount = betAmount;
    }

    public int getCrashMultiplier() {
        return crashMultiplier;
    }

    public void setCrashMultiplier(int crashMultiplier) {
        this.crashMultiplier = crashMultiplier;
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

    public boolean isCashedOut() {
        return cashedOut;
    }

    public void setCashedOut(boolean cashedOut) {
        this.cashedOut = cashedOut;
    }
}
