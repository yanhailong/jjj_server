package com.jjg.game.sim.data;

/**
 * slots 每次旋转上报给 sim 的统计明细 (经 ToSimBridge.onSlotsSpin 跨节点传输)。
 * <p>
 * 字段取自 slots 的 GameRunInfo, 用于经营信息 SPINE游戏面板的累计统计。
 *
 * @author 11
 * @date 2026/6/15
 */
public class SpinStatInfo {
    //本次投注 (GameRunInfo.stake)
    private long bet;
    //本次赢奖 (GameRunInfo.allWinGold)
    private long win;
    //本次中奖倍数 (GameRunInfo.allWinTimes)
    private int multiple;
    //大奖展示id (GameRunInfo.bigShowId; 0=无, 1.SWEET 2.BIG 3.MEGA 4.EPIC 5.LEGENDARY)
    private int bigShowId;
    //奖池金额 (>0 表示本次触发对应奖池)
    private long mini;
    private long minor;
    private long major;
    private long grand;
    //本次旋转后剩余免费次数 (用于检测免费模式触发)
    private int remainFreeCount;

    public SpinStatInfo() {
    }

    public long getBet() {
        return bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public long getWin() {
        return win;
    }

    public void setWin(long win) {
        this.win = win;
    }

    public int getMultiple() {
        return multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public int getBigShowId() {
        return bigShowId;
    }

    public void setBigShowId(int bigShowId) {
        this.bigShowId = bigShowId;
    }

    public long getMini() {
        return mini;
    }

    public void setMini(long mini) {
        this.mini = mini;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getGrand() {
        return grand;
    }

    public void setGrand(long grand) {
        this.grand = grand;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }
}
