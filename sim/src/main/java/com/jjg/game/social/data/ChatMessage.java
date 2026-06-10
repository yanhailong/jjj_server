package com.jjg.game.social.data;

/**
 * 聊天消息内存模型。
 * <p>
 * 世界/系统/联盟频道以 JSON 形式存入 Redis 有界列表; 私聊则映射到 {@link PrivateMessage} 落库。
 *
 * @author 11
 * @date 2026/6/9
 */
public class ChatMessage {
    //消息唯一id (snowflake, 用于保序/分页/去重)
    private long id;
    //频道 code (ChatChannelType.code)
    private int channel;
    //频道子id 比如房间id，联盟id
    private long channelSubId;
    //发送者id (系统消息为 0)
    private long fromId;
    //发送者昵称
    private String fromNick;
    //发送者头像id
    private int fromHeadImg;
    //发送者头像框id
    private int fromHeadFrame;
    //接收者id (私聊用; 其它频道为 0)
    private long toId;
    //消息内容
    private String content;
    //发送时间 (ms)
    private long time;

    public ChatMessage() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public long getChannelSubId() {
        return channelSubId;
    }

    public void setChannelSubId(long channelSubId) {
        this.channelSubId = channelSubId;
    }

    public long getFromId() {
        return fromId;
    }

    public void setFromId(long fromId) {
        this.fromId = fromId;
    }

    public String getFromNick() {
        return fromNick;
    }

    public void setFromNick(String fromNick) {
        this.fromNick = fromNick;
    }

    public int getFromHeadImg() {
        return fromHeadImg;
    }

    public void setFromHeadImg(int fromHeadImg) {
        this.fromHeadImg = fromHeadImg;
    }

    public int getFromHeadFrame() {
        return fromHeadFrame;
    }

    public void setFromHeadFrame(int fromHeadFrame) {
        this.fromHeadFrame = fromHeadFrame;
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
}
