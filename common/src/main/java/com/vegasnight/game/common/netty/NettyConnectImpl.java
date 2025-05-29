package com.vegasnight.game.common.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.0
 */
public class NettyConnectImpl extends NettyConnect {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void onClose() {
        log.debug("connect on closed.");
    }

    @Override
    public void onCreate() {
        log.debug("connect on create");
    }

    @Override
    public void messageReceived(Object msg) {
        log.debug("message received...");
    }
}
