package com.jjg.game.common.net;

/**
 * 连接监听器
 *
 * @author nobody
 * @since 1.0
 */
public interface ConnectListener {

    /**
     * 当连接被关闭
     */
    <T extends Connect<Object>> void onConnectClose(T connect);
}
