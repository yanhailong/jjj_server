package com.jjg.game.common.cluster;

import com.jjg.game.common.protostuff.PFMessage;

/**
 * 节点之间通讯消息的包装类
 *
 * @author nobody
 * @since 1.0
 */
public class ClusterMessage {
    private String sessionId;
    private final PFMessage msg;
    private long playerId;

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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public PFMessage getMsg() {
        return msg;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
}
