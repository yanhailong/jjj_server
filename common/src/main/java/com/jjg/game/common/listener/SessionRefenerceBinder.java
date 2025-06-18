package com.jjg.game.common.listener;

import com.jjg.game.common.protostuff.PFSession;

/**
 * @since 1.0
 */
public interface SessionRefenerceBinder {

    Object bind(PFSession session, long playerId);
}
