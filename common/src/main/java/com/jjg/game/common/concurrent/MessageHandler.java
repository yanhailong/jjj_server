package com.jjg.game.common.concurrent;

import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.PFSession;

/**
 * @author lm
 * @date 2025/11/19 11:41
 */
public abstract class MessageHandler<T> extends BaseHandler<T> {
    private final PFSession finalSession;
    private final PFMessage finalMsg;
    private final Connect<ClusterMessage> finalConnect;
    public MessageHandler(PFSession finalSession, PFMessage finalMsg, Connect<ClusterMessage> finalConnect) {
        this.finalSession = finalSession;
        this.finalMsg = finalMsg;
        this.finalConnect = finalConnect;
    }

    public PFSession getFinalSession() {
        return finalSession;
    }

    public PFMessage getFinalMsg() {
        return finalMsg;
    }

    public Connect<ClusterMessage> getFinalConnect() {
        return finalConnect;
    }
}
