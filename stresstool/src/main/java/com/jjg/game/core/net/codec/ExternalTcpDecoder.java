package com.jjg.game.core.net.codec;

import java.util.List;

import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.Pack;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.net.message.SMessage;
import com.jjg.game.core.processor.RobotLogicHandler;
import com.jjg.game.core.processor.RobotProcessorManager;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.core.robot.RobotThreadFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * 与机器人TCP协议解码
 *
 * @author 2CL
 */
public class ExternalTcpDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        byte[] array = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), array, 0, array.length);
        Pack pack = ProtostuffUtil.deserialize(array, Pack.class);
        PFMessage message = new PFMessage(pack.cmd, pack.data);
        RobotThread robot = RobotThreadFactory.getRobot(ctx.channel().id().asLongText());
        SMessage sMsg = SMessage.convertFromPfMsg(message);
        if (robot.getPlayer() == null || robot.getPlayer().getPlayerInfo() == null) {
            // 没有基本的玩家信息时,直接当前线程执行(登录请求Res[第一个Req请求对应的Res]返回后,会设置基本的玩家信息)
            // 登录请求返回,设置基本玩家数据后,根据玩家id选择对应的机器人逻辑线程执行
            robot.getWindow().getCtx().addReceiveMsgs();
            robot.addRespMsg(sMsg);
        } else {
            // 否则根据玩家id分发线程
            long playerId = robot.getPlayer().getPlayerInfo().getPid();
            RobotProcessorManager.getInstance()
                    .addRobotHandlerByPlayerId(playerId, new RobotLogicHandler(robot, sMsg));
        }
    }
}
