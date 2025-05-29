package com.vegasnight.game.common.listener;


/**
 * @since 1.0
 */
public interface SessionVerifyListener {

    void userVerifyPass(String sessionId, long playerId, String ip);
}
