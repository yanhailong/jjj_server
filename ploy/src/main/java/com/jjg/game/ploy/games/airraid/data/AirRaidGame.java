package com.jjg.game.ploy.games.airraid.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 空袭游戏数据 — 管理单局回合状态
 *
 * @author 11
 * @date 2026/3/27
 */
public class AirRaidGame {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //回合历史队列(保留最近20局的坠毁倍率)
    private final FixedSizeQueue<Integer> roundHistoryQueue = new FixedSizeQueue<>(20);
    //当前阶段
    private volatile AirRaidPhase phase = AirRaidPhase.BETTING;
    //回合计数器
    private final AtomicInteger roundCounter = new AtomicInteger(0);
    //本局坠毁倍率(万分比, 在回合开始时确定)
    private volatile int crashMultiplier;
    //当前实时倍率(万分比, 飞行期间持续增长, 起始 10000 即 1.00x)
    private volatile int currentMultiplier = 10000;
    //飞行阶段开始时间(ms)
    private volatile long flyStartTime;
    //当前阶段开始时间(ms)
    private volatile long phaseStartTime;


    /**
     * 开始新回合 — 重置状态
     *
     * @param crashMultiplier 本局坠毁倍率(万分比)
     */
    public void startNewRound(int crashMultiplier) {
        this.phase = AirRaidPhase.BETTING;
        this.crashMultiplier = crashMultiplier;
        this.currentMultiplier = 10000;
        this.flyStartTime = 0;
        this.phaseStartTime = System.currentTimeMillis();
        this.roundCounter.incrementAndGet();
        log.info("AirRaid round {} started, crashMultiplier={}", roundCounter.get(), crashMultiplier);
    }

    /**
     * 进入飞行阶段
     */
    public void startFlying() {
        this.phase = AirRaidPhase.FLYING;
        long now = System.currentTimeMillis();
        this.flyStartTime = now;
        this.phaseStartTime = now;
        log.info("AirRaid round {} flying", roundCounter.get());
    }

    /**
     * 坠毁 — 进入结算阶段，记录历史
     */
    public void crash() {
        this.phase = AirRaidPhase.CRASHED;
        this.currentMultiplier = crashMultiplier;
        this.phaseStartTime = System.currentTimeMillis();
        this.roundHistoryQueue.add(crashMultiplier);
        log.info("AirRaid round {} crashed at {}x", roundCounter.get(), crashMultiplier / 10000.0);
    }

    public FixedSizeQueue<Integer> getRoundHistoryQueue() {
        return roundHistoryQueue;
    }

    /**
     * 获取回合历史列表(不可变副本)
     */
    public List<Integer> getRoundHistoryList() {
        return List.copyOf(roundHistoryQueue);
    }

    public AirRaidPhase getPhase() {
        return phase;
    }

    public void setPhase(AirRaidPhase phase) {
        this.phase = phase;
    }

    public int getRoundCounter() {
        return roundCounter.get();
    }

    public int getCrashMultiplier() {
        return crashMultiplier;
    }

    public void setCrashMultiplier(int crashMultiplier) {
        this.crashMultiplier = crashMultiplier;
    }

    public int getCurrentMultiplier() {
        return currentMultiplier;
    }

    public void setCurrentMultiplier(int currentMultiplier) {
        this.currentMultiplier = currentMultiplier;
    }

    public long getFlyStartTime() {
        return flyStartTime;
    }

    public void setFlyStartTime(long flyStartTime) {
        this.flyStartTime = flyStartTime;
    }

    public long getPhaseStartTime() {
        return phaseStartTime;
    }

    public void setPhaseStartTime(long phaseStartTime) {
        this.phaseStartTime = phaseStartTime;
    }
}
