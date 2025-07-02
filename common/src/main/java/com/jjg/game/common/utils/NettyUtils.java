package com.jjg.game.common.utils;

import io.netty.channel.ChannelHandlerContext;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * netty 工具类
 *
 * @author 2CL
 */
public class NettyUtils {

    /**
     * 从ChannelContext中获取当前连接的URI
     */
    public static URI getUriFromContext(ChannelHandlerContext ctx) throws URISyntaxException {
        // 获取远程连接的 IP 和端口
        String remoteAddress = ctx.channel().remoteAddress().toString();

        // 检查地址格式是否符合 [IP:PORT]
        if (remoteAddress.startsWith("/")) {
            // 移除前置的 '/'
            remoteAddress = remoteAddress.substring(1);
        }

        // 拆分 IP 和端口
        String[] addressParts = remoteAddress.split(":");
        if (addressParts.length != 2) {
            throw new IllegalArgumentException("Invalid remote address: " + remoteAddress);
        }
        // 提取 IP 地址
        String host = addressParts[0];
        // 提取端口号
        int port = Integer.parseInt(addressParts[1]);

        // 组装为 URI 对象 (模板: ws://IP:PORT)
        return new URI("ws", null, host, port, null, null, null);
    }


    /**
     * 判断Netty连接是否有效
     *
     * @param ctx ctx
     * @return channel是否断开
     */
    public static boolean isDisconnectChannel(ChannelHandlerContext ctx) {
        return ctx == null
            || ctx.channel() == null
            || !ctx.channel().isActive()
            || !ctx.channel().isOpen();
    }
}
