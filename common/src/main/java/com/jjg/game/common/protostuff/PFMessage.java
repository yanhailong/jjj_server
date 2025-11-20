package com.jjg.game.common.protostuff;

import com.jjg.game.common.constant.MessageConst;

/**
 * 消息基础类定义
 * @since 1.0
 */
public class PFMessage {

    //消息类型
    public int messageType;
    //子命令字
    public int cmd;
    //数据
    public byte[] data;

    public PFMessage() {
    }

    public PFMessage(int cmd, byte[] data) {
        this.cmd = cmd;
        this.data = data;
        this.messageType = this.cmd >> MessageConst.MessageCommon.RIGHT_MOVE;
    }

    @Override
    public String toString() {
        return "PFMessage{" +
                ", cmd=0x" + Integer.toHexString(cmd) +
                '}';
    }

}
