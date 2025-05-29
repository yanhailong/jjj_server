package com.vegasnight.game.common.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import com.vegasnight.game.common.protostuff.PFMessage;
import com.vegasnight.game.common.protostuff.Pack;
import com.vegasnight.game.common.protostuff.ProtostuffUtil;
import com.vegasnight.game.common.utils.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @since 1.0
 */
public class WebSocketMessageEncoder extends MessageToMessageEncoder<PFMessage> {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Override
    protected void encode(ChannelHandlerContext ctx, PFMessage msg, List<Object> out) throws Exception {
        Pack p = new Pack(msg.cmd, msg.data);

        byte[] data = ProtostuffUtil.serialize(p);

        ByteBuf byteBuf = Unpooled.buffer(data.length);
        byteBuf.writeBytes(data);
        out.add(new BinaryWebSocketFrame(byteBuf));
    }
}
