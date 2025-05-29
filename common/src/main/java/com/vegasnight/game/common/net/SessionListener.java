package com.vegasnight.game.common.net;

/**
 * 回话监听器
 * <p>
 * @since 1.0
 */
public interface SessionListener {

    /**
     * 当回话被关闭
     *
     * @param session
     */
    void onSessionClose(Session session);

}
