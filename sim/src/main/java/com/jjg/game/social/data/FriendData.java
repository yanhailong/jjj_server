package com.jjg.game.social.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家社交关系数据 (每玩家一文档)。
 * <p>
 * 持久化策略: DB 权威 + 原子操作 (见 {@code FriendDao})。好友操作均为"用户动作级"低频写,
 * 跨玩家写(A 申请/赠送给 B)用 Mongo 字段级原子更新, 无需把对方载入内存, 天然规避并发竞态,
 * 也避免引入额外的内存缓存一致性问题。
 *
 * @author 11
 * @date 2026/6/9
 */
@Document("friendData")
public class FriendData {
    @Id
    private long playerId;
    //好友 friendId -> 关系条目
    private Map<Long, FriendEntry> friends = new HashMap<>();
    //黑名单 blackId -> 拉黑时间(ms)
    private Map<Long, Long> blacklist = new HashMap<>();
    //收到的好友申请 requesterId -> 申请时间(ms)
    private Map<Long, Long> pendingRequests = new HashMap<>();
    //待领取的赠礼 senderId -> 累计道具数量
    private Map<Long, Long> pendingGifts = new HashMap<>();
    //私聊会话清除标记 对端id -> 清除时间(ms); 单向删除会话用, 该时间点之前的消息对本人不再展示(不影响对方)
    private Map<Long, Long> conversationClear = new HashMap<>();
    //每日申请计数重置日 (yyyyMMdd)
    private int dailyRequestDay;
    //当日已发送申请数
    private int dailyRequestCount;
    //每日赠礼人数计数重置日 (yyyyMMdd)
    private int dailyGiftDay;
    //当日已赠送的不同好友人数 (用于每日赠送人数上限)
    private int dailyGiftPersonCount;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Long, FriendEntry> getFriends() {
        return friends;
    }

    public void setFriends(Map<Long, FriendEntry> friends) {
        this.friends = friends == null ? new HashMap<>() : friends;
    }

    public Map<Long, Long> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(Map<Long, Long> blacklist) {
        this.blacklist = blacklist == null ? new HashMap<>() : blacklist;
    }

    public Map<Long, Long> getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(Map<Long, Long> pendingRequests) {
        this.pendingRequests = pendingRequests == null ? new HashMap<>() : pendingRequests;
    }

    public Map<Long, Long> getPendingGifts() {
        return pendingGifts;
    }

    public void setPendingGifts(Map<Long, Long> pendingGifts) {
        this.pendingGifts = pendingGifts == null ? new HashMap<>() : pendingGifts;
    }

    public Map<Long, Long> getConversationClear() {
        return conversationClear;
    }

    public void setConversationClear(Map<Long, Long> conversationClear) {
        this.conversationClear = conversationClear == null ? new HashMap<>() : conversationClear;
    }

    /**
     * 取与某人会话的清除时间(ms), 无则 0。
     */
    public long conversationClearTime(long targetId) {
        if (conversationClear == null) {
            return 0;
        }
        Long t = conversationClear.get(targetId);
        return t == null ? 0 : t;
    }

    public int getDailyRequestDay() {
        return dailyRequestDay;
    }

    public void setDailyRequestDay(int dailyRequestDay) {
        this.dailyRequestDay = dailyRequestDay;
    }

    public int getDailyRequestCount() {
        return dailyRequestCount;
    }

    public void setDailyRequestCount(int dailyRequestCount) {
        this.dailyRequestCount = dailyRequestCount;
    }

    public int getDailyGiftDay() {
        return dailyGiftDay;
    }

    public void setDailyGiftDay(int dailyGiftDay) {
        this.dailyGiftDay = dailyGiftDay;
    }

    public int getDailyGiftPersonCount() {
        return dailyGiftPersonCount;
    }

    public void setDailyGiftPersonCount(int dailyGiftPersonCount) {
        this.dailyGiftPersonCount = dailyGiftPersonCount;
    }

    // ---------------------------------------------------------------------
    // 便捷判断
    // ---------------------------------------------------------------------

    public boolean isFriend(long id) {
        return friends != null && friends.containsKey(id);
    }

    public boolean isBlacklisted(long id) {
        return blacklist != null && blacklist.containsKey(id);
    }

    public int friendCount() {
        return friends == null ? 0 : friends.size();
    }

    public int blacklistCount() {
        return blacklist == null ? 0 : blacklist.size();
    }

    /**
     * 取当日已发送申请数 (跨天自动归零)。
     */
    public int currentDailyRequestCount(int today) {
        return this.dailyRequestDay == today ? this.dailyRequestCount : 0;
    }

    /**
     * 取当日已赠送的不同好友人数 (跨天自动归零)。
     */
    public int currentDailyGiftPersonCount(int today) {
        return this.dailyGiftDay == today ? this.dailyGiftPersonCount : 0;
    }

    /**
     * 获取排除黑名单的好友
     *
     * @return
     */
    public Map<Long, FriendEntry> friendsExcludeBlacklist() {
        if (this.friends == null || this.friends.isEmpty()) {
            return this.friends;
        }
        if (blacklist == null || blacklist.isEmpty()) {
            return this.friends;
        }
        Map<Long, FriendEntry> tmpFriends = new HashMap<>(this.friends);
        this.blacklist.keySet().forEach(tmpFriends::remove);
        return tmpFriends;
    }

    public boolean pending(long targetId) {
        if(this.pendingRequests == null || this.pendingRequests.isEmpty()) {
            return false;
        }
        return this.pendingRequests.containsKey(targetId);
    }
}
