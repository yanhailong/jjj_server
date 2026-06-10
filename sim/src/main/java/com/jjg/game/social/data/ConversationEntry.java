package com.jjg.game.social.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 私聊会话摘要 (单侧视角, 每个会话两条: A 看 B 一条 / B 看 A 一条)。
 * <p>
 * 由批量落库时增量维护 (最新消息 + 未读数), 会话列表直接按 playerId 查本表,
 * 替代"扫最近 N 条消息内存聚合 + 未读数聚合"的方案, 开销从 O(消息量) 降为 O(会话数)。
 * {@code updateTime} 建 TTL 索引, 与消息同寿命自动过期, 防止无界增长。
 *
 * @author 11
 * @date 2026/6/10
 */
@Document("social_conversation")
public class ConversationEntry {
    //{playerId}_{targetId}
    @Id
    private String id;
    //本侧玩家
    private long playerId;
    //对端玩家
    private long targetId;
    //最新一条消息id
    private long lastMsgId;
    //最新一条消息发送者
    private long lastFromId;
    //最新一条消息内容
    private String lastContent;
    //最新一条消息时间(ms)
    private long lastTime;
    //对端发给我的未读数
    private int unread;
    //最后活跃时间 (TTL 索引, 与消息保留期一致自动过期)
    private Date updateTime;

    public ConversationEntry() {
    }

    /**
     * 摘要主键 (有方向: 本侧在前)。
     */
    public static String id(long playerId, long targetId) {
        return playerId + "_" + targetId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public long getLastMsgId() {
        return lastMsgId;
    }

    public void setLastMsgId(long lastMsgId) {
        this.lastMsgId = lastMsgId;
    }

    public long getLastFromId() {
        return lastFromId;
    }

    public void setLastFromId(long lastFromId) {
        this.lastFromId = lastFromId;
    }

    public String getLastContent() {
        return lastContent;
    }

    public void setLastContent(String lastContent) {
        this.lastContent = lastContent;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
