package com.jjg.game.common.net;

/**
 * @since 1.0
 */
public interface Inbox<T> {

    void onClusterReceive(Connect connect, T message);
}
