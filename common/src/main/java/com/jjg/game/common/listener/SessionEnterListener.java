package com.jjg.game.common.listener;

import com.jjg.game.common.protostuff.PFSession;

/**
 * 当session进入时调用
 *
 * @author nobody
 * @since 1.0
 */
public interface SessionEnterListener {

    void sessionEnter(PFSession session, long playerId);
}
