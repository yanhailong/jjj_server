package com.jjg.game.common.concurrent;

/**
 * @author lm
 * @date 2025/11/14 11:37
 */

import com.lmax.disruptor.EventFactory;

public class PlayerEventFactory implements EventFactory<PlayerEvent> {
    @Override
    public PlayerEvent newInstance() {
        return new PlayerEvent();
    }
}
