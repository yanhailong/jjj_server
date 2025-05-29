package com.vegasnight.game.common.net;

import com.google.protobuf.GeneratedMessage;
import com.vegasnight.game.common.protostuff.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户session
 *
 * @scene 1.0
 */
public class MarsSession extends Session<GeneratedMessage, PFMessage> implements Inbox<PFMessage> {

    protected Logger log = LoggerFactory.getLogger(getClass());


    public MarsSession(String id, NetAddress netAddress, Connect connect) {
        super(id, connect, netAddress);
    }

    @Override
    public void send(GeneratedMessage gmsg) {
        
    }

    @Override
    public void onClusterReceive(Connect connect, PFMessage message) {
    }
}
