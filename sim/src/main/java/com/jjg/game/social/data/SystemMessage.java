package com.jjg.game.social.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 玩家定向系统消息。目前每个玩家仅保存一条注册欢迎消息。
 */
@Document("social_system_msg")
public class SystemMessage {
    //玩家 id 同时作为主键，保证欢迎消息只落库一次
    @Id
    private long playerId;
    //消息雪花 id
    private long messageId;
    //消息内容
    private String content;
    //发送时间(ms, 客户端展示/排序)
    private long time;
    //创建时间(用于 TTL 索引)
    private Date createTime;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
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
}
