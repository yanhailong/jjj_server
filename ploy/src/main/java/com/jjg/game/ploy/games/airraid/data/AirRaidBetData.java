package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.core.constant.GameConstant;

/**
 * 空袭游戏单个注单
 * 每个玩家最多持有2个注单(betIndex 0/1)
 * <p>
 * 所有对该数据的读写都在玩家所属的 disruptor 分区线程上完成
 * (workId 派发,见 ClusterMessageDispatcher / AirRaidPloyController.handleAutoCashOutTimer / doSettle),
 * 因此无需任何同步语义。
 *
 * @author 11
 * @date 2026/3/27
 */
public class AirRaidBetData {
    //投注金额
    private long betAmount;
    //是否已兑现
    private boolean cashedOut;
    //兑现时的倍率(万分比, 10000 = 1.00x)
    private int cashOutMultiplier;
    //赢得金额
    private long winAmount;
    //自动兑现目标倍率(万分比, 0 表示未启用); 在下注时从玩家配置快照而来
    private int autoCashOutTarget;

    public AirRaidBetData(long betAmount) {
        this.betAmount = betAmount;
    }

    /**
     * 兑现：标记已兑现，计算赢得金额
     * winAmount = betAmount × multiplier / 10000
     *
     * @param multiplier 兑现倍率(万分比, 10000 = 1.00x)
     */
    public void cashOut(int multiplier) {
        this.cashedOut = true;
        this.cashOutMultiplier = multiplier;
        this.winAmount = betAmount * multiplier / GameConstant.TEN_THOUSAND;
    }

    /**
     * 回滚兑现 — 派奖明确失败时使用(winFromPool 抛异常时不允许调用,见 doCashOut 文档)
     */
    public void rollbackCashOut() {
        this.cashedOut = false;
        this.cashOutMultiplier = 0;
        this.winAmount = 0;
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

    public int getCashOutMultiplier() {
        return cashOutMultiplier;
    }

    public long getWinAmount() {
        return winAmount;
    }

    public int getAutoCashOutTarget() {
        return autoCashOutTarget;
    }

    public void setAutoCashOutTarget(int autoCashOutTarget) {
        this.autoCashOutTarget = autoCashOutTarget;
    }
}
