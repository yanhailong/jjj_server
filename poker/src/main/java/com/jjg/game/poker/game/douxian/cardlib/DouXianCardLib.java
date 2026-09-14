package com.jjg.game.poker.game.douxian.cardlib;

import com.jjg.game.poker.game.common.cardlib.CardLibEntry;

import java.util.List;

/**
 * 斗仙牌首轮结果库条目。只保存四个模拟座位各8张初始手牌，不保存剩余20张牌的顺序。
 * multiplier 是指定真人座位集合经过多次完整四回合模拟后的平均净输赢倍数。
 */
public class DouXianCardLib implements CardLibEntry {

    private long multiplier;
    private int realPlayerCount;
    private List<Integer> realSeatIndexes;
    private List<List<Integer>> seatCards;
    private List<Long> seatExpectedMultipliers;
    private long profitStdDev;
    private int rolloutCount;
    private int winRateBps;
    private int simulatorVersion;

    public DouXianCardLib() {
    }

    public DouXianCardLib(long multiplier, int realPlayerCount, List<Integer> realSeatIndexes,
                          List<List<Integer>> seatCards, List<Long> seatExpectedMultipliers,
                          long profitStdDev, int rolloutCount, int winRateBps, int simulatorVersion) {
        this.multiplier = multiplier;
        this.realPlayerCount = realPlayerCount;
        this.realSeatIndexes = realSeatIndexes;
        this.seatCards = seatCards;
        this.seatExpectedMultipliers = seatExpectedMultipliers;
        this.profitStdDev = profitStdDev;
        this.rolloutCount = rolloutCount;
        this.winRateBps = winRateBps;
        this.simulatorVersion = simulatorVersion;
    }

    @Override
    public long getMultiplier() {
        return multiplier;
    }

    @Override
    public String getPartitionKey() {
        return "real-" + realPlayerCount;
    }

    public int getRealPlayerCount() {
        return realPlayerCount;
    }

    public List<Integer> getRealSeatIndexes() {
        return realSeatIndexes;
    }

    public List<List<Integer>> getSeatCards() {
        return seatCards;
    }

    public List<Long> getSeatExpectedMultipliers() {
        return seatExpectedMultipliers;
    }

    public long getProfitStdDev() {
        return profitStdDev;
    }

    public int getRolloutCount() {
        return rolloutCount;
    }

    public int getWinRateBps() {
        return winRateBps;
    }

    public int getSimulatorVersion() {
        return simulatorVersion;
    }
}
