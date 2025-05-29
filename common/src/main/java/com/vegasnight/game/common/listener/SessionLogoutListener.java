package com.vegasnight.game.common.listener;

/**
 * @since 1.0
 */
public interface SessionLogoutListener {
    void logout(long playerId, String sessionId);
}
