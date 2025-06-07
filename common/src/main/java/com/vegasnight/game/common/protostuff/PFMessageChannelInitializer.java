package com.vegasnight.game.common.protostuff;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * 消息头固定字节数消息处理器，本类采用固定4字节头(int),头信息不包含头本身字节数的方式定义
 * 消息包大小限定为2M类，超出后会抛出异常
 *
 * @scene 1.0
 */
public class PFMessageChannelInitializer
        extends ChannelInitializer<SocketChannel> {

    private static final int MSG_MAX_SIZE = 2 * 1024 * 1024;

    private static final int HANDER_SIZE = 4;

    private ChannelHandler messageDispatcher;

    private NioEventLoopGroup workGroup = new NioEventLoopGroup();

    public PFMessageChannelInitializer(ChannelHandler messageDispatcher) {
        super();
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        // 解码用
        p.addLast(new LengthFieldBasedFrameDecoder(
                MSG_MAX_SIZE, 0, HANDER_SIZE, 0, 4));
        p.addLast(new PFMessageDecoder());
        // 编码用
        p.addLast(new LengthFieldPrepender(HANDER_SIZE));
        p.addLast(new PFMessageEncoder());
        p.addLast(workGroup, messageDispatcher);
    }

}
