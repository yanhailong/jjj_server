package com.jjg.game.common.cluster;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.message.ClusterRegsiterMsg;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;

/**
 * 集群节点连接对象
 *
 * @author nobody
 * @since 1.0
 */
public class ClusterConnect extends NettyConnect<ClusterMessage> implements Connect<ClusterMessage> {

    private final ClusterMessageDispatcher clusterMessageDispatcher;

    private final ClusterMessage clusterHeartBeatMessage =
        new ClusterMessage(new PFMessage(MessageConst.ToClientConst.REQ_HEART_BEAT, null));

    public ClusterConnect(ClusterMessageDispatcher clusterMessageDispatcher) {
        this.clusterMessageDispatcher = clusterMessageDispatcher;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent e) {
            //长时间未收到消息
            if (e.state() == IdleState.READER_IDLE) {
                //log.debug("读取消息闲置,ctx={}" + ctx);
                //ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                //长时间未写出消息
                // log.debug("节点请求发送心跳信息,ctx={}", ctx);
                write(clusterHeartBeatMessage);
                // ctx.close();
            } else {
                log.debug("连接空闲时间到,关闭连接,ctx={}", ctx);
                ctx.close();
            }
        }
    }

    @Override
    public void messageReceived(ClusterMessage msg) {
        if (msg.getMsg().cmd == MessageConst.ToClientConst.RES_HEART_BEAT) {
            //log.debug("收到心跳回包消息,ctx={}", ctx);
        } else {
            clusterMessageDispatcher.onClusterReceive(this, msg);
        }
    }

    @Override
    public void onClose() {
        //TODO
    }

    @Override
    public void onCreate() {
        ClusterRegsiterMsg clusterRegsiterMsg = new ClusterRegsiterMsg();
        clusterRegsiterMsg.nodePath = ClusterSystem.system.nodeManager.getNodePath();
        if (clusterRegsiterMsg.nodePath != null) {
            PFMessage pfMessage = MessageUtil.getPFMessage(clusterRegsiterMsg);
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            write(clusterMessage);
        }
    }
}
