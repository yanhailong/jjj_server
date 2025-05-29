package com.vegasnight.game.common.cluster;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @since 1.0
 */
public class ClusterConnectWorkPoolInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MSG_MAX_SIZE = 10 * 1024 * 1024;

    private static final int HANDER_SIZE = 4;
    private ClusterMessageDispacher clusterMessageDispacher;

    public ClusterConnectWorkPoolInitializer(ClusterMessageDispacher clusterMessageDispacher) {
        this.clusterMessageDispacher = clusterMessageDispacher;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                MSG_MAX_SIZE, 0, HANDER_SIZE, 0, 4))
                .addLast(new ClusterMessageDecoder())
                .addLast(new LengthFieldPrepender(HANDER_SIZE))
                .addLast(new ClusterMessageEncoder())
                .addLast("idleStateHandler", new IdleStateHandler(60, 20, 30, TimeUnit.SECONDS))
                .addLast(new NioEventLoopGroup(), new ClusterConnect(clusterMessageDispacher));
    }
}
