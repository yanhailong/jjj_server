package com.jjg.game.common.cluster;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 节点连接通道初始化器
 *
 * @author noboy
 * @since 1.0
 */
public class ClusterConnectInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MSG_MAX_SIZE = 10 * 1024 * 1024;

    private static final int HANDLER_SIZE = 4;
    private final ClusterMessageDispatcher clusterMessageDispatcher;

    public ClusterConnectInitializer(ClusterMessageDispatcher clusterMessageDispatcher) {
        this.clusterMessageDispatcher = clusterMessageDispatcher;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                MSG_MAX_SIZE, 0, HANDLER_SIZE, 0, 4))
            .addLast(new ClusterMessageDecoder())
            .addLast(new LengthFieldPrepender(HANDLER_SIZE))
            .addLast(new ClusterMessageEncoder())
            .addLast("idleStateHandler", new IdleStateHandler(60, 20, 30, TimeUnit.SECONDS))
            .addLast(new ClusterConnect(clusterMessageDispatcher));
    }
}
