package com.jjg.game.core.net.codec;

import com.jjg.game.common.utils.NettyUtils;
import com.jjg.game.core.net.connect.GameRobotClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * websocket客户端handler
 *
 * @author 2CL
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientHandler.class);
    private WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebSocketClientHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PongWebSocketFrame frame) {
            ctx.channel().write(new PingWebSocketFrame(frame.content().retain()));
            log.info("收到服务端" + ctx.channel().remoteAddress() + "发来的心跳：PONG");
        } else if (msg instanceof TextWebSocketFrame frame) {
            // 接收服务端发送过来的消息
            log.info("收到服务端" + ctx.channel().remoteAddress() + "发来的消息：" + frame.text());
        } else if (msg instanceof BinaryWebSocketFrame frame) {
            ExternalTcpDecoder decoder = new ExternalTcpDecoder();
            decoder.decode(ctx, frame.content(), new ArrayList<>());
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端下线");
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("websocket客户端连接异常", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            log.debug("握手成功");
        } else if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
            log.debug("握手超时");
        }
    }
}