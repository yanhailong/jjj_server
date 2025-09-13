package com.jjg.game.slots.handler;

import com.jjg.game.core.listener.GmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 11
 * @date 2025/9/12 15:59
 */
public abstract class SlotsMessageHandler implements GmListener {
    protected final Logger log;

    public SlotsMessageHandler() {
        this.log = LoggerFactory.getLogger(getClass());
    }
}
