package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.ploy.data.PloyGameRoom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 空袭游戏房间 — 管理单局回合状态
 * <p>
 * 所有节点(主节点/从节点)各持有一份实例，主节点通过集群消息同步状态到从节点。
 * </p>
 *
 * @author 11
 * @date 2026/3/27
 */
public class AirRaidGameRoom extends PloyGameRoom {

    //回合历史队列(保留最近20局的坠毁倍率, 万分比)
    private final FixedSizeQueue<Integer> roundHistoryQueue = new FixedSizeQueue<>(20);
    //当前阶段
    private volatile AirRaidPhase phase = AirRaidPhase.BETTING;
    //当前阶段开始时间(ms)
    private long phaseStartTime;
    //当前阶段结束时间(ms)，用于所有节点按统一截止点做本地裁决
    private long phaseStopTime;
    //阶段变化是否已通知(避免重复广播)
    private boolean notifyPhase;
    //回合计数器
    private final AtomicInteger roundCounter = new AtomicInteger(0);
    //本局坠毁倍率(万分比, 在飞行开始前确定)
    private volatile int crashMultiplier;
    //当前实时倍率(万分比, 飞行期间持续增长, 起始 10000 即 1.00x)
    private volatile int currentMultiplier = 10000;

    public FixedSizeQueue<Integer> getRoundHistoryQueue() {
        return roundHistoryQueue;
    }

    public AirRaidPhase getPhase() {
        return phase;
    }

    public void setPhase(AirRaidPhase phase) {
        this.phase = phase;
    }

    public long getPhaseStartTime() {
        return phaseStartTime;
    }

    public void setPhaseStartTime(long phaseStartTime) {
        this.phaseStartTime = phaseStartTime;
    }

    public long getPhaseStopTime() {
        return phaseStopTime;
    }

    public void setPhaseStopTime(long phaseStopTime) {
        this.phaseStopTime = phaseStopTime;
    }

    public boolean isNotifyPhase() {
        return notifyPhase;
    }

    public void setNotifyPhase(boolean notifyPhase) {
        this.notifyPhase = notifyPhase;
    }

    public AtomicInteger getRoundCounter() {
        return roundCounter;
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

    public int getRoundId() {
        return roundCounter.get();
    }

    /**
     * 开始新回合 — 重置所有状态，进入下注阶段
     */
    public void startNewRound() {
        this.phase = AirRaidPhase.BETTING;
        this.currentMultiplier = 10000;
        this.crashMultiplier = 0;
        this.phaseStartTime = System.currentTimeMillis();
        this.phaseStopTime = 0;
        this.notifyPhase = false;
        this.roundCounter.incrementAndGet();
    }

    /**
     * 进入飞行阶段 — 倍率从1.00x开始增长
     *
     * @param crashMultiplier 本局坠毁倍率(万分比)
     * @param crashTimeSec    本局坠毁时间(秒)
     */
    public void startFlying(int crashMultiplier, int crashTimeSec) {
        this.phase = AirRaidPhase.FLYING;
        this.crashMultiplier = crashMultiplier;
        this.currentMultiplier = 10000;
        this.phaseStartTime = System.currentTimeMillis();
        this.phaseStopTime = this.phaseStartTime + crashTimeSec * 1000L;
        this.notifyPhase = false;
    }

    /**
     * 飞机坠毁 — 进入结算阶段，记录历史
     */
    public void crash() {
        this.phase = AirRaidPhase.CRASHED;
        this.currentMultiplier = this.crashMultiplier;
        this.phaseStartTime = System.currentTimeMillis();
        this.notifyPhase = false;
        this.roundHistoryQueue.add(this.crashMultiplier);
    }

    /**
     * 获取回合历史列表的防御性副本
     *
     * @return 最近20局坠毁倍率列表(万分比)
     */
    public List<Integer> getRoundHistoryList() {
        return new ArrayList<>(roundHistoryQueue);
    }

    /**
     * 使用权威同步状态更新本地房间状态。
     *
     * @return true 表示状态已应用；false 表示同步消息过期，被忽略
     */
    public synchronized boolean applyAuthoritativeState(int roundId, AirRaidPhase phase, long phaseStartTime,
                                                        long phaseStopTime,
                                                        int currentMultiplier, int crashMultiplier) {
        int currentRoundId = getRoundId();
        if (roundId < currentRoundId) {
            return false;
        }
        if (roundId == currentRoundId && phase.getCode() < this.phase.getCode()) {
            return false;
        }

        roundCounter.set(roundId);
        this.phase = phase;
        this.phaseStartTime = phaseStartTime;
        this.phaseStopTime = phaseStopTime;
        this.currentMultiplier = currentMultiplier;
        this.crashMultiplier = crashMultiplier;
        this.notifyPhase = true;
        return true;
    }

    public synchronized void replaceRoundHistory(List<Integer> roundHistory) {
        roundHistoryQueue.clear();
        if (roundHistory != null) {
            roundHistory.forEach(roundHistoryQueue::add);
        }
    }

    /**
     * 检查该时间能否下注
     * @param time
     * @return
     */
    public boolean canBet(long time) {
        return getRoundId() > 0 && phase == AirRaidPhase.BETTING && phaseStopTime > 0 && time < phaseStopTime;
    }

    /**
     * 检查该时间能否兑现
     * @param time
     * @return
     */
    public boolean canCashOut(long time) {
        return getRoundId() > 0 && phase == AirRaidPhase.FLYING && this.phaseStopTime > 0 && time < this.phaseStopTime;
    }
}
