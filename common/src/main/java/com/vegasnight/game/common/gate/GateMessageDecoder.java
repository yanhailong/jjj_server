package com.vegasnight.game.common.gate;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import com.vegasnight.game.common.protostuff.PFMessage;

import java.util.List;

/**
 * 消息解码器
 * @since 1.0
 */
public class GateMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int cmd = msg.readUnsignedShort();
        byte[] array = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), array, 0, array.length);
        PFMessage message = new PFMessage(cmd, array);
        out.add(message);
    }
}
