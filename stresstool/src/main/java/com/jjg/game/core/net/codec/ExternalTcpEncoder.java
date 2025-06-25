package com.jjg.game.core.net.codec;

import com.jjg.game.common.protostuff.Pack;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.net.message.SMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * TCP协议编码
 *
 * @author 2CL
 */
public class ExternalTcpEncoder extends MessageToMessageEncoder<Object> {

    private static final Logger log = LoggerFactory.getLogger(ExternalTcpEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        System.out.println("加密消息");
        SMessage sMessage = (SMessage) msg;
        Pack pack = new Pack(sMessage.getId(), sMessage.getData());

        byte[] data = ProtostuffUtil.serialize(pack);

        ByteBuf byteBuf = Unpooled.buffer(data.length);
        byteBuf.writeBytes(data);

        out.add(new BinaryWebSocketFrame(byteBuf));
    }
}
