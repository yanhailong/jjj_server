package com.jjg.game.core.net.codec;

import com.jjg.game.core.robot.StressRobotManager;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.core.robot.RobotThreadFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端netty handler
 *
 * @author 2CL
 */
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeChannel(ctx);
        super.channelInactive(ctx);
    }

    private void closeChannel(ChannelHandlerContext ctx) {
        RobotThread robot = RobotThreadFactory.removeRobot(ctx.channel().id().asLongText());
        if (robot != null) {
            robot.getWindow().getCtx().closeConnection(robot);
            StressRobotManager.instance().remove(ctx.channel().id().asLongText());
        }
    }
}
