package com.jjg.game.common.netty;

import com.jjg.game.common.net.Connect;
import com.jjg.game.common.net.ConnectListener;
import com.jjg.game.common.net.NetAddress;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketException;
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
        //cause.printStackTrace();
        if (cause instanceof SocketException) {

        } else {
            log.error("tips caught exception,ctx={}", ctx, cause);
        }
        close();
    }

    @Override
    public boolean write(Object msg) {
        try {
            ctx.writeAndFlush(msg).addListener(future -> {
                if (!future.isSuccess()) {
                    Throwable e = future.cause();
                    if (e != null) {
                        log.error("发送消息出现异常", e);
                        throw new RuntimeException("发送消息出现异常", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("发送消息出现异常", e);
            throw new RuntimeException("发送消息出现异常", e);
        }

        return true;
    }

    /**
     * 发送消息并添加自定义监听
     */
    public void writeWithFuture(Object msg, GenericFutureListener<? extends Future<? super Void>> future) {
        try {
            ctx.writeAndFlush(msg).addListener(future);
        } catch (Exception e) {
            log.error("发送消息出现异常", e);
            throw new RuntimeException("发送消息出现异常", e);
        }
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
            ctx.writeAndFlush(obj).addListener(future -> {
                if (isActive()) {
                    // TODO 玩家不在线处理流程
                    ctx.close();
                }
            });
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
