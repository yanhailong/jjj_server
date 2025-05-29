package com.vegasnight.game.common.ws;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @since 1.0
 */
public class WebSocketChildChannelHandler extends ChannelInitializer<SocketChannel> {

    private int timeOutSecond = 30;

    public WebSocketChildChannelHandler() {
    }

    public WebSocketChildChannelHandler(int timeOutSecond) {
        this.timeOutSecond = timeOutSecond;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // TODO Auto-generated method stub
        ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(timeOutSecond, 0, 0, TimeUnit.SECONDS));
        ch.pipeline().addLast("http-codec", new HttpServerCodec());
        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
        ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
        ch.pipeline().addLast("encoder", new WebSocketMessageEncoder());
        ch.pipeline().addLast("handler", new WebSocketServerHandler());
    }
}
