package com.jjg.game.common.listener;

import com.jjg.game.common.protostuff.PFSession;

/**
 * 对session中的引用值进行绑定{@linkplain com.jjg.game.common.net.Session#reference}
 *
 * @author nobody
 * @since 1.0
 */
public interface SessionReferenceBinder {

    Object bind(PFSession session, long playerId);
}
