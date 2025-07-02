package com.jjg.game.common.net;

import com.jjg.game.common.cluster.ClusterMessage;

/**
 * @since 1.0
 */
public interface Inbox<T> {

    void onClusterReceive(Connect<ClusterMessage> connect, T message);
}
