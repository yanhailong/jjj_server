package com.vegasnight.game.common.cluster;

import com.vegasnight.game.common.config.NodeConfig;
import com.vegasnight.game.common.curator.MarsNode;
import com.vegasnight.game.common.net.Connect;
import com.vegasnight.game.common.netty.ConnectPool;

/**
 * 集群客户端对象
 * @since 1.0
 */
public class ClusterClient {
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
    public ConnectPool connectPool;

    private ClusterSystem clusterSystem;

    public ClusterClient(MarsNode marsNode, ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
        init(marsNode);
    }

    public void init(MarsNode marsNode) {
        this.marsNode = marsNode;
        nodeConfig = marsNode.getNodeConfig();
        this.connectPool = clusterSystem.getMarsConnectPool(nodeConfig.getTcpAddress());
    }

    public Connect getConnect() throws InterruptedException {
        return connectPool.getConnect();
    }

    public Connect getConnectSync() throws InterruptedException {
        return connectPool.getConnectSync();
    }

    public void write(Object msg) throws InterruptedException {
        connectPool.getConnect().write(msg);
    }

    public boolean canReceive(int messageType) {
        if (marsNode != null && marsNode.getNodeConfig() != null && marsNode.getNodeConfig().getMicserviceMessageTypes() != null) {
            return marsNode.getNodeConfig().getMicserviceMessageTypes().contains(messageType);
        }
        return false;
    }

    public String getType() {
        return nodeConfig.getType();
    }

    public void close(Connect connect) {
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
