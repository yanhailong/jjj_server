package com.vegasnight.game.common.protostuff;

import com.vegasnight.game.common.proto.ProtobufMessage;

import java.util.Arrays;

/**
 * 发送给客户端的消息结构
 *
 * @since 1.0
 */
@ProtobufMessage
public class Pack {
    //命令号
    public int cmd;
    //数据
    public byte[] data;

    public Pack() {
    }

    public Pack(int cmd, byte[] data) {
        this.cmd = cmd;
        this.data = data;
    }

    @Override
    public String toString() {
        return "PFToClientMessage{" +
                "cmd=" + cmd +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
