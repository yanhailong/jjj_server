package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 邮件
 * @author 11
 * @date 2025/8/11 17:17
 */
@Document
public class Mail implements Cloneable{
    @Id
    private long id;
    //所属玩家ID
    @Indexed
    private long playerId;
    //邮件标题
    private String title;
    //邮件内容
    private String content;
    //发送邮件的时间
    private int sendTime;
    //超时时间
    private int timeout;
    //是否为全服邮件
    private boolean serverMail;
    //邮件中的道具列表
    private List<Item> items;
    //邮件状态 0.未阅读  1.已阅读  2.已领取
    private int status;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSendTime() {
        return sendTime;
    }

    public void setSendTime(int sendTime) {
        this.sendTime = sendTime;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isServerMail() {
        return serverMail;
    }

    public void setServerMail(boolean serverMail) {
        this.serverMail = serverMail;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public Mail clone() throws CloneNotSupportedException {
        return (Mail)super.clone();
    }
}
