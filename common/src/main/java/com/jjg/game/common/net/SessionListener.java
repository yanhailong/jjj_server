package com.jjg.game.common.net;

/**
 * 回话监听器
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
