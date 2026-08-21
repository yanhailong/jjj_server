package com.jjg.game.social.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 好友私聊消息 (落库, 保留 7 天)。
 * <p>
 * {@code conversationId = min(a,b)_max(a,b)} 把一对玩家的双向消息归到同一会话, 便于按会话分页拉取。
 * {@code createTime} 为 BSON Date, 建 TTL 索引由 Mongo 自动清理过期消息 ——
 * 满足"保留 7 天 / 分页拉取不受条数封顶"且无需服务端定时清理。
 *
 * @author 11
 * @date 2026/6/9
 */
@Document("social_private_msg")
public class PrivateMessage {
    //雪花id (主键 + 分页游标)
    @Id
    private long id;
    //会话id min_max
    private String conversationId;
    //发送者
    private long fromId;
    //接收者
    private long toId;
    //内容
    private String content;
    //发送时间(ms, 客户端展示/排序)
    private long time;
    //创建时间(用于 TTL 索引, 7 天自动过期)
    private Date createTime;
    //接收方是否已读
    private volatile boolean read;

    public PrivateMessage() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public long getFromId() {
        return fromId;
    }

    public void setFromId(long fromId) {
        this.fromId = fromId;
    }

    public long getToId() {
        return toId;
    }

    public void setToId(long toId) {
        this.toId = toId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * 构建一对玩家的会话id (与顺序无关)。
     */
    public static String conversationId(long a, long b) {
        return a < b ? (a + "_" + b) : (b + "_" + a);
    }
}
