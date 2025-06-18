package com.jjg.game.common.gate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import com.jjg.game.common.protostuff.PFMessage;

import java.util.List;

/**
 * 消息编码器
 * @since 1.0
 */
public class GateMessageEncoder extends MessageToMessageEncoder<PFMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, PFMessage msg, List<Object> out) throws Exception {
        int len = 0;
        if (msg.data != null) {
            len = msg.data.length;
        }
        ByteBuf byteBuf = Unpooled.buffer(len + 4);
        byteBuf.writeShort(msg.messageType);
        byteBuf.writeShort(msg.cmd);
        byteBuf.writeBytes(msg.data);
        out.add(byteBuf);
    }
}
