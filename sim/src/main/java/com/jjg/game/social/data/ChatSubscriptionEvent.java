package com.jjg.game.social.data;

import java.util.List;

/**
 * 聊天订阅消息的跨节点投递载荷。
 */
public class ChatSubscriptionEvent {
    //客户端消息号
    private int cmd;
    //客户端消息体 (Base64)
    private String data;
    //限定接收玩家; null 表示全部聊天订阅者
    private List<Long> targetPlayerIds;

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<Long> getTargetPlayerIds() {
        return targetPlayerIds;
    }

    public void setTargetPlayerIds(List<Long> targetPlayerIds) {
        this.targetPlayerIds = targetPlayerIds;
    }
}
