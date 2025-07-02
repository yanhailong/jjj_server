package com.jjg.game.common.gate;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * netty初始化链配置
 *
 * @author nobody
 * @since 1.0
 */
public class GateChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MSG_MAX_SIZE = 10 * 1024 * 1024;

    private static final int HANDLER_SIZE = 4;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                MSG_MAX_SIZE, 0, HANDLER_SIZE, 0, 4))
            .addLast(new GateMessageDecoder())
            .addLast(new LengthFieldPrepender(HANDLER_SIZE))
            .addLast(new GateMessageEncoder())
            .addLast(new GateSession());
    }
}
