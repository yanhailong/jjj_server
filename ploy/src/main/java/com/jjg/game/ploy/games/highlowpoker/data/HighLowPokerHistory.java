package com.jjg.game.ploy.games.highlowpoker.data;

import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 10:32
 */
public class HighLowPokerHistory {
    //本局历史记录 牌id,赔率
    private List<HighLowHistoryInfo> history;
    //总营收
    private long totalProfit;

    public List<HighLowHistoryInfo> getHistory() {
        return history;
    }

    public void setHistory(List<HighLowHistoryInfo> history) {
        this.history = history;
    }

    public long getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(long totalProfit) {
        this.totalProfit = totalProfit;
    }
}
