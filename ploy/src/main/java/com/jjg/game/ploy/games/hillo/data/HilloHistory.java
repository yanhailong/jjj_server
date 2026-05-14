package com.jjg.game.ploy.games.hillo.data;

import java.util.List;

public class HilloHistory {
    private List<HilloHistoryInfo> history;
    private long totalProfit;
    private long startTime;
    private long bet;
    private int betMode;
    private long balanceAfter;

    public List<HilloHistoryInfo> getHistory() {
        return history;
    }

    public void setHistory(List<HilloHistoryInfo> history) {
        this.history = history;
    }

    public long getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(long totalProfit) {
        this.totalProfit = totalProfit;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getBet() {
        return bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public int getBetMode() {
        return betMode;
    }

    public void setBetMode(int betMode) {
        this.betMode = betMode;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
}
