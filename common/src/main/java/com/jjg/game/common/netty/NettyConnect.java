package com.jjg.game.common.netty;

import com.jjg.game.common.net.Connect;
import com.jjg.game.common.net.ConnectListener;
import com.jjg.game.common.net.NetAddress;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * netty 连接抽象类
 *
 * @author nobody
 * @since 1.0
 */
public abstract class NettyConnect<T> extends SimpleChannelInboundHandler<T> implements Connect<T> {

    private static final Logger log = LoggerFactory.getLogger(NettyConnect.class);
    protected ChannelHandlerContext ctx;
    protected NetAddress remoteAddress;
    protected List<ConnectListener> connectListeners = new CopyOnWriteArrayList<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
        log.debug("连接创建完成,ctx={}", ctx);
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        remoteAddress = new NetAddress(address.getAddress().getHostAddress(), address.getPort());
        onCreate();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.error("Netty caught exception,ctx={}", ctx, cause);
        close();
    }

    @Override
    public boolean write(Object msg) {
        if (ctx == null || !ctx.channel().isActive()) {
            ReferenceCountUtil.release(msg);
            return false;
        }
        if (!ctx.channel().isWritable()) {
            log.warn("channel not writable, drop msg, remote={}", remoteAddress);
            ReferenceCountUtil.release(msg);
            return false;
        }
        try {
            ctx.writeAndFlush(msg).addListener((Future<? super Void> f) -> {
                if (!f.isSuccess()) {
                    log.error("发送消息失败, remote={}", remoteAddress, f.cause());
                    ReferenceCountUtil.release(msg);
                    ctx.close();
                }
            });
        } catch (Exception e) {
            log.error("发送消息出现异常", e);
            ctx.close();
        }
        return true;
    }

    /**
     * 发送消息并添加自定义监听
     */
    public void writeWithFuture(Object msg, GenericFutureListener<Future<? super Void>> listener) {
        if (!ctx.channel().isActive()) {
            ReferenceCountUtil.release(msg);
            return;
        }
        ctx.writeAndFlush(msg).addListener((Future<? super Void> f) -> {
            try {
                listener.operationComplete(f);
            } catch (Throwable t) {
                log.error("future listener error", t);
            }
            if (!f.isSuccess()) {
                ReferenceCountUtil.release(msg);
                ctx.close();
            }
        });
    }


    @Override
    public boolean isActive() {
        return ctx.channel().isActive();
    }

    @Override
    public NetAddress address() {
        return remoteAddress;
    }

    @Override
    public void close() {
        log.debug("服务器主动关闭连接,netAddress={},ctx={}", remoteAddress, ctx);
        try {
            ctx.close();
        } catch (Exception e) {
            log.warn("关闭连接异常,netAddress=" + remoteAddress + ",ctx=" + ctx, e);
        }
    }


    public void writeAndClose(Object obj) {
        log.debug("服务器主动关闭连接并通知,netAddress={},ctx={}", remoteAddress, ctx);
        try {
            if (!ctx.channel().isActive()) {
                ReferenceCountUtil.release(obj);
                return;
            }
            ctx.writeAndFlush(obj).addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            log.error("关闭连接异常,netAddress={},ctx={}", remoteAddress, ctx, e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
        messageReceived(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channel inactive,ctx={}", ctx);
        connectListeners.forEach(connectListener -> connectListener.onConnectClose((Connect<Object>) this));
        connectListeners.clear();
        onClose();
        super.channelInactive(ctx);
    }

    /**
     * 当消息到达
     *
     * @param msg
     */
    public abstract void messageReceived(T msg);

    @Override
    public void addConnectListener(ConnectListener connectListener) {
        connectListeners.add(connectListener);
    }

    @Override
    public void removeConnectListener(ConnectListener connectListener) {
        connectListeners.remove(connectListener);
    }

    @Override
    public String toString() {
        return "NettyConnect{" +
                "ctx=" + ctx +
                ", remoteAddress=" + remoteAddress +
                '}';
    }
}
