package com.jjg.game.core.net.codec;

import com.jjg.game.common.protostuff.Pack;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.net.message.SMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * websocket 解码器
 *
 * @author 2CL
 */
public class WebSocketMsgEncoder extends MessageToMessageEncoder<SMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, SMessage msg, List<Object> out) throws Exception {
        Pack p = new Pack(msg.getId(), msg.getData());

        byte[] data = ProtostuffUtil.serialize(p);

        ByteBuf byteBuf = Unpooled.buffer(data.length);
        byteBuf.writeBytes(data);
        out.add(new BinaryWebSocketFrame(byteBuf));
    }
}
