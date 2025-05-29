package com.vegasnight.game.common.net;

/**
 * 连接监听器
 * <p>
 * @since 1.0
 */
public interface ConnectListener {

    /**
     * 当连接被关闭
     *
     * @param connect
     */
    void onConnectClose(Connect connect);
}
