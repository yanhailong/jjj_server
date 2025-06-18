package com.jjg.game.common.net;

/**
 * 连接监听器
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
