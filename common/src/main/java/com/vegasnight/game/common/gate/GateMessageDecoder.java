package com.vegasnight.game.common.gate;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import com.vegasnight.game.common.protostuff.PFMessage;

import java.util.List;

/**
 * @since 1.0
 */
public class GateMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // copy the ByteBuf content to a byte array
        //int messageType = msg.readUnsignedShort();
        int cmd = msg.readUnsignedShort();
        byte[] array = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), array, 0, array.length);
        PFMessage message = new PFMessage(cmd, array);
        out.add(message);
    }
}
