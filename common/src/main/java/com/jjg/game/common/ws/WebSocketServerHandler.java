package com.jjg.game.common.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import com.jjg.game.common.gate.GateSession;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.Pack;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nobody
 * @since 1.0
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    Logger log = LoggerFactory.getLogger(getClass());

    public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }

    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        return false;
    }

    /**
     * 当客户端连接成功，返回个成功信息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        GateSession gateSession = new WSGateSession();
        gateSession.channelActive(ctx);
        GateSession.getGateSessionMap().put(gateSession.getSessionId(), gateSession);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    String sessionId = ctx.channel().id().asShortText();
                    GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
                    if (gateSession != null) {
                        log.warn("连接读闲置时间到，即将被关闭,activeTime={},ctx={}", gateSession.getActiveTime(), ctx);
                    } else {
                        log.warn("连接读闲置时间到，即将被关闭,ctx={},sessionId={}", ctx, sessionId);
                    }
                    ctx.close();
                    break;
                case WRITER_IDLE:
                case ALL_IDLE:
                default:
                    break;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 当客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().id().asShortText();
        GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
        if (gateSession != null) {
            gateSession.channelInactive(ctx);
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        ctx.flush();
    }

    public String getRealIp(FullHttpRequest request) {
        if (request.headers().contains("X-Forwarded-For")) {
            return request.headers().get("X-Forwarded-For");
        } else if (request.headers().contains("x-forwarded-for")) {
            return request.headers().get("x-forwarded-for");
        } else if (request.headers().contains("HTTP_X_FORWARDED_FOR")) {
            return request.headers().get("HTTP_X_FORWARDED_FOR");
        } else if (request.headers().contains("X-Real_IP")) {
            return request.headers().get("X-Real_IP");
        } else {
            return request.headers().get("X-Real_IP");
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof FullHttpRequest fullHttpRequest) {
            //log.debug("收到消息，session={},msg={}", ctx.channel().id(), msg);
            String ip = getRealIp(fullHttpRequest);
            if (ip != null && !ip.isEmpty()) {
                if (ip.contains(":")) {
                    ip = ip.split(":")[0];
                } else if (ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                log.debug("ws 获取到用户真实ip={}", ip);
                AttributeKey<String> attributeKey = AttributeKey.valueOf("X-Real_IP");
                Attribute<String> attribute = ctx.channel().attr(attributeKey);
                attribute.set(ip);
            }
            handleHttpRequest(ctx, fullHttpRequest);
        } else if (msg instanceof WebSocketFrame) {
            //ws://xxxx
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        //关闭请求
        if (frame instanceof CloseWebSocketFrame) {
            channelInactive(ctx);
            return;
        }
        //ping请求
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //支持二进制消息
        if (frame instanceof BinaryWebSocketFrame) {
//            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            ByteBuf msg = frame.content();
            decode(ctx, msg);
        }
    }

    public void decode(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            String sessionId = ctx.channel().id().asShortText();
            GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
            if (gateSession != null) {
                byte[] array = new byte[msg.readableBytes()];

                msg.getBytes(msg.readerIndex(), array, 0, array.length);

                Pack pack = ProtostuffUtil.deserialize(array, Pack.class);
                PFMessage message = new PFMessage(pack.cmd, pack.data);
                gateSession.messageReceived(message);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    //第一次请求是http请求，请求头包括ws的信息
    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws:/" + ctx.channel() + "/websocket", null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            //不支持
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            log.debug("返回握手信息: {}", req);
        }
    }

    //异常处理，netty默认是关闭channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        // TODO Auto-generated method stub
        //输出日志
        // cause.printStackTrace();
        // 网络异常时，应当抛出实际异常日志，方便定位问题
        log.error(cause.getLocalizedMessage(), cause);
        ctx.close();
    }
}
