package com.jjg.game.common.cluster;

import com.jjg.game.common.protostuff.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * 节点消息解码器
 * @since 1.0
 */
public class ClusterMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // copy the ByteBuf content to a byte array
        byte[] array = ByteBufUtil.getBytes(msg);
        ClusterMessage deserialize = ProtostuffUtil.deserialize(array, ClusterMessage.class);
        out.add(deserialize);
    }
}
