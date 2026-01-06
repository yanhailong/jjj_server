package com.jjg.game.common.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 采用Netty4.x 实现NIO服务器，该类继承了Thread 线程类，应用过程中如果需要
 * 异步开启网络服务，可以调用start方法，否则直接调用run。
 *
 * @author NOBODY
 * @since 1.0
 */
public class NettyServer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    /**
     * 服务监听端口
     */
    private int portNumber;
    /**
     * 服务监听地址
     */
    private String address;
    /**
     * 连接初始化器
     */
    private ChannelInitializer<SocketChannel> initializer;

    private ServerBootstrap b;

    public NettyServer(int portNumber, ChannelInitializer<SocketChannel> initializer) {
        this(null, portNumber, initializer);
    }

    public NettyServer(String address, int portNumber, ChannelInitializer<SocketChannel> initializer) {
        super("netty-server-" + portNumber);
        this.address = address;
        this.portNumber = portNumber;
        this.initializer = initializer;
    }

    @Override
    public void run() {
        // 在linux环境中EPoll的表现比NIO的性能表现更好
        // Netty NIO使用的是Selector，其底层在Linux上通过select或poll（取决于实现）；
        //select/poll 的问题：每次都要轮询所有file Descriptor（文件描述符），时间复杂度 O(n)；
        //而 epoll 是事件驱动的，事件发生时由内核主动通知用户态，复杂度是 O(1)。
        EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        Class<? extends ServerChannel> channelCls = Epoll.isAvailable() ? EpollServerSocketChannel.class :
            NioServerSocketChannel.class;
        //workerGroup.setIoRatio(30);
        try {
            this.b = new ServerBootstrap();
            this.b.group(bossGroup, workerGroup);
            this.b.channel(channelCls);
            this.b.childHandler(this.initializer).childOption(ChannelOption.SO_KEEPALIVE, true)
                // ChannelOption.TCP_NODELAY 是否禁用Nagle算法(是否减小TCP网络中小数据包数量).ture禁用,降低延迟,false启用,提高吞吐
                .childOption(ChannelOption.TCP_NODELAY, true)
                //建议设置到4K
                .option(ChannelOption.SO_BACKLOG, 4096)
                .handler(new LoggingHandler(LogLevel.INFO));
            // 服务器绑定端口监听
            ChannelFuture f;
            if (this.address != null) {
                f = this.b.bind(this.address, this.portNumber).sync();
                log.info("Server started on address = {}, port = {}", this.address, this.portNumber);
            } else {
                f = this.b.bind("0.0.0.0", this.portNumber).sync();
                log.info("Server started on port = {}", this.portNumber);
            }
            // 监听服务器关闭监听
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("\nnet server start error...\n", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
