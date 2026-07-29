package com.jjg.game.sim.data;

import java.util.List;
import java.util.Map;

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
    //本次旋转各档奖池的触发次数 (档位 1.MINI 2.MINOR 3.MAJOR 4.GRAND; 一次旋转可同时命中多档或同档多次)
    //奖池中奖金额已计入 win, 各档金额只有客户端展示要用, 不跨节点传
    private Map<Integer, Long> jackpotCounts;
    //本次旋转后剩余免费次数 (用于检测免费模式触发)
    private int remainFreeCount;
    //本次结果库的模式类型集合 (SlotsResultLib.libTypeSet, SpecialMode 表 type; 赛季试炼任务判定用)
    private List<Integer> specialModes;
    //本次旋转的图标 (SlotsResultLib.iconArr; 赛季试炼任务判定图标出现次数用)
    private List<Integer> icons;
    //本次旋转的幂等 id (slots 侧生成, 非 0; sim 侧凭此拒绝超时重试的重复投递)
    private long spinId;
    //本次旋转是否处于免费模式
    private boolean freeMode;
    //是否为触发免费
    private boolean triggerFree;

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

    public Map<Integer, Long> getJackpotCounts() {
        return jackpotCounts;
    }

    public void setJackpotCounts(Map<Integer, Long> jackpotCounts) {
        this.jackpotCounts = jackpotCounts;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    public List<Integer> getSpecialModes() {
        return specialModes;
    }

    public void setSpecialModes(List<Integer> specialModes) {
        this.specialModes = specialModes;
    }

    public List<Integer> getIcons() {
        return icons;
    }

    public void setIcons(List<Integer> icons) {
        this.icons = icons;
    }

    public long getSpinId() {
        return spinId;
    }

    public void setSpinId(long spinId) {
        this.spinId = spinId;
    }

    public boolean isFreeMode() {
        return freeMode;
    }

    public void setFreeMode(boolean freeMode) {
        this.freeMode = freeMode;
    }

    public boolean isTriggerFree() {
        return triggerFree;
    }

    public void setTriggerFree(boolean triggerFree) {
        this.triggerFree = triggerFree;
    }
}
