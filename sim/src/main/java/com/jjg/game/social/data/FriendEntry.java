package com.jjg.game.social.data;

/**
 * 单个好友关系条目 (内嵌于 {@link FriendData#getFriends()})。
 *
 * @author 11
 * @date 2026/6/9
 */
public class FriendEntry {
    //成为好友的时间 (ms)
    private long addTime;
    //最近一次向该好友赠送礼物的日期 (yyyyMMdd, 与 giftSendCount 配合做每日次数判定; 跨天即视为 0 次)
    private int lastGiftSendDay;
    //lastGiftSendDay 当日已向该好友赠送的次数 (用于每人每日赠送次数上限)
    private int giftSendCount;

    public FriendEntry() {
    }

    public FriendEntry(long addTime) {
        this.addTime = addTime;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public int getLastGiftSendDay() {
        return lastGiftSendDay;
    }

    public void setLastGiftSendDay(int lastGiftSendDay) {
        this.lastGiftSendDay = lastGiftSendDay;
    }

    public int getGiftSendCount() {
        return giftSendCount;
    }

    public void setGiftSendCount(int giftSendCount) {
        this.giftSendCount = giftSendCount;
    }

    /**
     * 取在 today 当日已向该好友赠送的次数 (跨天自动归零)。
     */
    public int currentGiftSendCount(int today) {
        return this.lastGiftSendDay == today ? this.giftSendCount : 0;
    }
}
