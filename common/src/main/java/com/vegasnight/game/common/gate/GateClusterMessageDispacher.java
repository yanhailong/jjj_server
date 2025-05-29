package com.vegasnight.game.common.gate;

import io.netty.channel.ChannelHandler;
import com.vegasnight.game.common.cluster.ClusterMessage;
import com.vegasnight.game.common.cluster.ClusterMessageDispacher;
import com.vegasnight.game.common.cluster.ClusterSystem;
import com.vegasnight.game.common.net.Connect;
import com.vegasnight.game.common.protostuff.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.0
 */
@ChannelHandler.Sharable
public class GateClusterMessageDispacher extends ClusterMessageDispacher {

    private Logger log = LoggerFactory.getLogger(getClass());

    public GateClusterMessageDispacher(ClusterSystem clusterSystem) {
        super(clusterSystem);
    }

    public void onClusterReceive(Connect connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.sessionId;
        PFMessage pfMessage = clusterMessage.msg;
        if (sessionId != null && !sessionId.isEmpty()) {
            GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
            if (gateSession != null) {
                gateSession.onClusterReceive(connect, pfMessage);
                return;
            } else {
                log.warn("找不到sessionId={}的session，无法转发消息", sessionId);
            }
        } else {
            if (pfMessage != null && pfMessage.cmd == 2 && pfMessage.messageType == 1) {
                return;
            }
            super.handle(connect, null, pfMessage);
        }
    }
}
