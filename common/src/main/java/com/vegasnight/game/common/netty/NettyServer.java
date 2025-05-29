package com.vegasnight.game.common.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 采用Netty4.x 实现NIO服务器，该类继承了Thread 线程类，应用过程中如果需要
 * <p>
 * 异步开启网络服务，可以调用start方法，否则直接调用run。
 * <p>
 *
 * @since 1.0
 */
public class NettyServer extends Thread {

    private Logger log = LoggerFactory.getLogger(getClass());

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

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        //workerGroup.setIoRatio(30);
        try {
            this.b = new ServerBootstrap();
            this.b.group(bossGroup, workerGroup);
            this.b.channel(NioServerSocketChannel.class);
            this.b.childHandler(this.initializer).childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 4096)//建议设置到4K
                    .handler(new LoggingHandler(LogLevel.INFO));
            // 服务器绑定端口监听
            ChannelFuture f;
            if (this.address != null) {
                f = this.b.bind(this.address, this.portNumber).sync();
                this.log.info("Server started on address = {}, port = {}", this.address, this.portNumber);
            } else {
                f = this.b.bind(this.portNumber).sync();
                this.log.info("Server started on port = {}" , this.portNumber);
            }
            // 监听服务器关闭监听
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            this.log.error("\nnet server start error...\n", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
