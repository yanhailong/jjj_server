package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/6/12 17:33
 */
public class BroadcastMsg {
    private Object msg;
    private String exceptPlayerId;

    public Object getMsg() {
        return msg;
    }

    public void setMsg(Object msg) {
        this.msg = msg;
    }

    public String getExceptPlayerId() {
        return exceptPlayerId;
    }

    public void setExceptPlayerId(String exceptPlayerId) {
        this.exceptPlayerId = exceptPlayerId;
    }
}
