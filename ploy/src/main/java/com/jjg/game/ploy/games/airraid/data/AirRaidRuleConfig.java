package com.jjg.game.ploy.games.airraid.data;


/**
 * @author 11
 * @date 2026/5/14
 */
public class AirRaidRuleConfig {
    //倍数增长率 r (万分比, 如1200=12%)
    private int growthRate;
    //风险增量 k (万分比/秒, 如60=0.6%)
    private int riskK;
    //初始坠毁率 p0 (万分比, 如300=3%)
    private int crashP0;
    //下注阶段时长(ms)
    private int bettingDurationMs;
    //停止下注时长(ms)
    private int stopBetDurationMs;
    //结算阶段时长(ms)
    private int settleDurationMs;

    public int getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(int growthRate) {
        this.growthRate = growthRate;
    }

    public int getRiskK() {
        return riskK;
    }

    public void setRiskK(int riskK) {
        this.riskK = riskK;
    }

    public int getCrashP0() {
        return crashP0;
    }

    public void setCrashP0(int crashP0) {
        this.crashP0 = crashP0;
    }

    public int getBettingDurationMs() {
        return bettingDurationMs;
    }

    public void setBettingDurationMs(int bettingDurationMs) {
        this.bettingDurationMs = bettingDurationMs;
    }

    public int getStopBetDurationMs() {
        return stopBetDurationMs;
    }

    public void setStopBetDurationMs(int stopBetDurationMs) {
        this.stopBetDurationMs = stopBetDurationMs;
    }

    public int getSettleDurationMs() {
        return settleDurationMs;
    }

    public void setSettleDurationMs(int settleDurationMs) {
        this.settleDurationMs = settleDurationMs;
    }
}
