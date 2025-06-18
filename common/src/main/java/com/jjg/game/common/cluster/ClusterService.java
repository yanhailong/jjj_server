package com.jjg.game.common.cluster;

import com.jjg.game.common.net.NetAddress;

/**
 * 集群服务
 * @since 1.0
 */
public interface ClusterService {

    /**
     * 用户进入
     *
     * @param sessionId
     * @param netAddress
     * @return
     */
    boolean sessionEnter(String sessionId, NetAddress netAddress);

    /**
     * 用户退出
     * 
     * @param sessionId
     * @return
     */
    boolean sessionQuit(String sessionId);

    /**
     * 用户消息到达
     * 
     * @param sessionId
     * @param msg
     */
    void messageReceive(String sessionId, Object msg);
}
