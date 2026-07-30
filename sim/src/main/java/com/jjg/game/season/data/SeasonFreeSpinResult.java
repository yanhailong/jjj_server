package com.jjg.game.season.data;

/**
 * 赛季每日免费局申请结果 (slots 经 ToSimBridge.useSeasonFreeSpin 同步获取)。
 * free=true 表示本次旋转已消耗一次免费次数, slots 侧不再扣下注额。
 *
 * @author 11
 * @date 2026/7/17
 */
public class SeasonFreeSpinResult {
    /** 非本赛季机台/阶段未开放/功能未配置: slots 可清除候选标记 */
    public static final int REASON_UNAVAILABLE = 1;
    /** 今日免费次数已用完: slots 可按天缓存, 当日不再申请 */
    public static final int REASON_EXHAUSTED = 2;
    private boolean free;
    private int remaining;
    private int reason;

    public boolean isFree() { return free; }
    public void setFree(boolean free) { this.free = free; }
    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    public int getReason() { return reason; }
    public void setReason(int reason) { this.reason = reason; }
}
