package com.vegasnight.game.common.cluster;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import com.vegasnight.game.common.protostuff.ProtostuffUtil;

import java.util.List;

/**
 * @since 1.0
 */
public class ClusterMessageEncoder extends MessageToMessageEncoder<ClusterMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ClusterMessage msg, List<Object> out) throws Exception {
        out.add(Unpooled.wrappedBuffer(ProtostuffUtil.serialize(msg)));
    }
}
