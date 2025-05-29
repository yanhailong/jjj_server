package com.vegasnight.game.common.cluster;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.message.ClusterRegsiterMsg;
import com.vegasnight.game.common.net.Connect;
import com.vegasnight.game.common.netty.NettyConnect;
import com.vegasnight.game.common.protostuff.MessageUtil;
import com.vegasnight.game.common.protostuff.PFMessage;

/**
 * @since 1.0
 */
public class ClusterConnect extends NettyConnect<ClusterMessage> implements Connect {

    private ClusterMessageDispacher clusterMessageDispacher;
    ClusterMessage clusterMessage = new ClusterMessage(new PFMessage(MessageConst.ToClientConst.REQ_HEART_BEAT, null));
    public ClusterConnect(ClusterMessageDispacher clusterMessageDispacher) {
        this.clusterMessageDispacher = clusterMessageDispacher;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            //长时间未收到消息
            if (e.state() == IdleState.READER_IDLE) {
                //log.debug("读取消息闲置,ctx={}" + ctx);
                //ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {//长时间未写出消息
                //log.debug("写消息闲置,ctx={}" + ctx);
                write(clusterMessage);
                // ctx.close();
            } else {
                log.debug("连接空闲时间到,ctx={}",ctx);
                ctx.close();
            }
        }
    }

    @Override
    public void messageReceived(ClusterMessage msg) {
        if (msg.msg.cmd == MessageConst.ToClientConst.RES_HEART_BEAT) {
            //log.debug("收到心跳回包消息,ctx={}" + ctx);
        } else {
            clusterMessageDispacher.onClusterReceive(this, msg);
        }
    }

    @Override
    public void onClose() {
        //TODO
    }

    @Override
    public void onCreate() {
        ClusterRegsiterMsg clusterRegsiterMsg = new ClusterRegsiterMsg();
        clusterRegsiterMsg.nodePath = ClusterSystem.system.nodeManager.nodePath;
        if (clusterRegsiterMsg.nodePath != null) {
            PFMessage pfMessage = MessageUtil.getPFMessage(clusterRegsiterMsg);
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            write(clusterMessage);
        }
    }
}
