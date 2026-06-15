package com.jjg.game.sim.data;

/**
 * 单个 slots 游戏的累计统计数据 (按 gameType 存于 {@link SimCasinoData#slotStatsMap}).
 * <p>
 * 数据来源: slots 每次旋转经 ToSimBridge.onSlotsSpin 上报的 {@link SpinStatInfo}.
 *
 * @author 11
 * @date 2026/6/15
 */
public class SlotGameStatsData {
    //投注总数 (累计)
    private long totalBet;
    //SPINE次数 (累计旋转次数)
    private long spinCount;
    //总赢奖 (累计)
    private long totalWin;
    //最高赢奖 (单次最大)
    private long maxWin;
    //最高倍数 (单次最大)
    private int maxMultiple;
    //大奖触发次数 (SlotsConst.BigWinShow: 1.SWEET 2.BIG 3.MEGA 4.EPIC 5.LEGENDARY)
    private long sweetWin;
    private long bigWin;
    private long megaWin;
    private long epicWin;
    private long legendaryWin;
    //奖池触发次数
    private long miniCount;
    private long minorCount;
    private long majorCount;
    private long grandCount;
    //免费游戏触发次数
    private long freeCount;
    //上次上报的剩余免费次数 (用于检测 0->>0 的免费模式触发)
    private int lastRemainFree;

    public long getTotalBet() {
        return totalBet;
    }

    public void setTotalBet(long totalBet) {
        this.totalBet = totalBet;
    }

    public long getSpinCount() {
        return spinCount;
    }

    public void setSpinCount(long spinCount) {
        this.spinCount = spinCount;
    }

    public long getTotalWin() {
        return totalWin;
    }

    public void setTotalWin(long totalWin) {
        this.totalWin = totalWin;
    }

    public long getMaxWin() {
        return maxWin;
    }

    public void setMaxWin(long maxWin) {
        this.maxWin = maxWin;
    }

    public int getMaxMultiple() {
        return maxMultiple;
    }

    public void setMaxMultiple(int maxMultiple) {
        this.maxMultiple = maxMultiple;
    }

    public long getSweetWin() {
        return sweetWin;
    }

    public void setSweetWin(long sweetWin) {
        this.sweetWin = sweetWin;
    }

    public long getBigWin() {
        return bigWin;
    }

    public void setBigWin(long bigWin) {
        this.bigWin = bigWin;
    }

    public long getMegaWin() {
        return megaWin;
    }

    public void setMegaWin(long megaWin) {
        this.megaWin = megaWin;
    }

    public long getEpicWin() {
        return epicWin;
    }

    public void setEpicWin(long epicWin) {
        this.epicWin = epicWin;
    }

    public long getLegendaryWin() {
        return legendaryWin;
    }

    public void setLegendaryWin(long legendaryWin) {
        this.legendaryWin = legendaryWin;
    }

    public long getMiniCount() {
        return miniCount;
    }

    public void setMiniCount(long miniCount) {
        this.miniCount = miniCount;
    }

    public long getMinorCount() {
        return minorCount;
    }

    public void setMinorCount(long minorCount) {
        this.minorCount = minorCount;
    }

    public long getMajorCount() {
        return majorCount;
    }

    public void setMajorCount(long majorCount) {
        this.majorCount = majorCount;
    }

    public long getGrandCount() {
        return grandCount;
    }

    public void setGrandCount(long grandCount) {
        this.grandCount = grandCount;
    }

    public long getFreeCount() {
        return freeCount;
    }

    public void setFreeCount(long freeCount) {
        this.freeCount = freeCount;
    }

    public int getLastRemainFree() {
        return lastRemainFree;
    }

    public void setLastRemainFree(int lastRemainFree) {
        this.lastRemainFree = lastRemainFree;
    }
}
