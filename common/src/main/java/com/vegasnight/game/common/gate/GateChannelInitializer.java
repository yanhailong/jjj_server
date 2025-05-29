package com.vegasnight.game.common.gate;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * @since 1.0
 */
public class GateChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MSG_MAX_SIZE = 10 * 1024 * 1024;

    private static final int HANDER_SIZE = 4;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                MSG_MAX_SIZE, 0, HANDER_SIZE, 0, 4))
                .addLast(new GateMessageDecoder())
                .addLast(new LengthFieldPrepender(HANDER_SIZE))
                .addLast(new GateMessageEncoder())
//                .addLast("idleStateHandler", new IdleStateHandler(25, 25, 20, TimeUnit.SECONDS))
//                .addLast(new ChannelDuplexHandler() {
//                    @Override
//                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//                        if (evt instanceof IdleStateEvent) {
//                            IdleStateEvent e = (IdleStateEvent) evt;
//                            if (e.state() == IdleState.ALL_IDLE) {
//                                ctx.close();
//                            }
//                        }
//                    }
//                })
                .addLast(new GateSession());
    }
}
