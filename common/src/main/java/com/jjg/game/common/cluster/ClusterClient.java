package com.jjg.game.common.cluster;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.netty.ConnectPool;
import com.jjg.game.common.netty.NettyConnect;

/**
 * 集群客户端对象
 *
 * @author nobody
 * @since 1.0
 */
public class ClusterClient {
    private final ClusterSystem clusterSystem;
    /**
     * 节点的配置信息
     */
    public NodeConfig nodeConfig;
    /**
     * 集群节点信息
     */
    public MarsNode marsNode;
    /**
     * 连接池
     */
    public ConnectPool<NettyConnect<Object>> connectPool;

    public ClusterClient(MarsNode marsNode, ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
        init(marsNode);
    }

    public void init(MarsNode marsNode) {
        this.marsNode = marsNode;
        nodeConfig = marsNode.getNodeConfig();
        this.connectPool = clusterSystem.getMarsConnectPool(nodeConfig.getTcpAddress());
    }

    public NettyConnect<Object> getConnect() throws InterruptedException {
        return connectPool.getConnect();
    }

    public NettyConnect<Object> getConnectSync() throws InterruptedException {
        return connectPool.getConnectSync();
    }

    public void write(Object msg) throws InterruptedException {
        connectPool.getConnect().write(msg);
    }

    public String getType() {
        return nodeConfig.getType();
    }

    public void close(NettyConnect<Object> connect) {
        connectPool.close(connect);
        if (connect != null) {
            connect.close();
        }
    }

    public void shutdown() {
        if (connectPool != null) {
            connectPool.shutdown();
        }
    }

}
