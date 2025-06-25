package com.jjg.game.core.net.message;

import com.jjg.game.common.protostuff.PFMessage;

/**
 * 发送消息
 */
public class SMessage {

    public SMessage(int id, byte[] data) {
        this.id = id;
        this.data = data;
        this.status = "";
    }

    public SMessage(int id, byte[] data, int resOrder) {
        this.id = id;
        this.data = data;
        this.resOrder = resOrder;
        this.status = "";
    }

    public SMessage(int id, String status) {
        this.id = id;
        this.status = status;
        this.data = new byte[0];
    }

    public SMessage(SMessageFactory factory) {
        this.factory = factory;
    }

    public static SMessage convertFromPfMsg(PFMessage pfMessage) {
        return new SMessage(pfMessage.cmd, pfMessage.data);
    }

    public int getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * 消息工厂
     */
    private SMessageFactory factory;

    /**
     * 消息id *
     */
    private int id;
    /**
     * 消息内容 *
     */
    private byte[] data;
    /**
     * 状态码字符串 空-正常 非空-异常 *
     */
    private String status = "";
    /**
     * 默认为 -1
     *
     * <p>-2 为没有对应的response驱动
     */
    private int resOrder = -2;

    public void reuse() {
        if (factory != null) {
            factory.recycleSMessage(this);
        }
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void clear() {
        this.data = null;
        this.status = "";
    }

    public int getResOrder() {
        return resOrder;
    }
}
