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
    //最近一次向该好友赠送礼物的日期 (yyyyMMdd, 用于每日赠送一次的判定)
    private int lastGiftSendDay;

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
}
