package com.vegasnight.game.common.listener;

import com.vegasnight.game.common.protostuff.PFSession;

/**
 * @since 1.0
 */
public interface SessionEnterListener {

    void sessionEnter(PFSession session, long playerId);
}
