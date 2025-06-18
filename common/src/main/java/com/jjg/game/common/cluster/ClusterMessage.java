package com.jjg.game.common.cluster;

import com.jjg.game.common.protostuff.PFMessage;

/**
 * 节点之间通讯消息的包装类
 * @since 1.0
 */
public class ClusterMessage {
    public String sessionId;
    public PFMessage msg;
    public long playerId;

    public ClusterMessage(PFMessage msg) {
        this.msg = msg;
    }

    public ClusterMessage(String sessionId, PFMessage msg) {
        this.sessionId = sessionId;
        this.msg = msg;
    }

    public ClusterMessage(String sessionId, PFMessage msg, long playerId) {
        this.sessionId = sessionId;
        this.msg = msg;
        this.playerId = playerId;
    }
}
