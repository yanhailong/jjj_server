package com.jjg.game.common.gate;

import io.netty.channel.ChannelHandler;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterMessageDispatcher;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.protostuff.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群消息分发
 * @author nobody
 * @since 1.0
 */
@ChannelHandler.Sharable
public class GateClusterMessageDispatcher extends ClusterMessageDispatcher {

    private Logger log = LoggerFactory.getLogger(getClass());

    public GateClusterMessageDispatcher(ClusterSystem clusterSystem) {
        super(clusterSystem);
    }

    @Override
    public void onClusterReceive(Connect<ClusterMessage> connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.getSessionId();
        PFMessage pfMessage = clusterMessage.getMsg();
        if (sessionId != null && !sessionId.isEmpty()) {
            // 节点消息
            GateSession gateSession = GateSession.getGateSessionMap().get(sessionId);
            if (gateSession != null) {
                gateSession.onClusterReceive(connect, pfMessage);
            } else {
                log.warn("找不到sessionId={}的session，无法转发消息, playerId = {}, pfMessage = {}", sessionId, clusterMessage.getPlayerId(),pfMessage);
            }
        } else {
            // 网关消息
            if (pfMessage != null && pfMessage.cmd == 2 && pfMessage.messageType == 1) {
                return;
            }
            super.handle(connect, null, pfMessage);
        }
    }
}
