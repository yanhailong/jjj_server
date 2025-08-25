package com.jjg.game.common.rpc;

import com.jjg.game.common.cluster.ClusterClient;

import java.util.ArrayList;
import java.util.List;

/**
 * rcp参数
 *
 * @author 2CL
 */
public class RpcReqParameterBuilder {

    // 需要发送的节点客户端
    private List<ClusterClient> clusterClients = new ArrayList<>();
    // 每个节点客户端重试次数
    private int retryTimesPerClient;
    // 每个节点客户端尝试时间
    private int tryMillisPerClient;

    public static RpcReqParameterBuilder create() {
        return new RpcReqParameterBuilder();
    }

    public List<ClusterClient> getClusterClients() {
        return clusterClients;
    }

    public RpcReqParameterBuilder addClusterClient(ClusterClient clusterClients) {
        this.clusterClients.add(clusterClients);
        return this;
    }

    public RpcReqParameterBuilder setClusterClients(List<ClusterClient> clusterClients) {
        this.clusterClients = clusterClients;
        return this;
    }

    public int getRetryTimesPerClient() {
        return retryTimesPerClient;
    }

    public RpcReqParameterBuilder setRetryTimesPerClient(int retryTimesPerClient) {
        this.retryTimesPerClient = retryTimesPerClient;
        return this;
    }

    public int getTryMillisPerClient() {
        return tryMillisPerClient;
    }

    public RpcReqParameterBuilder setTryMillisPerClient(int tryMillisPerClient) {
        this.tryMillisPerClient = tryMillisPerClient;
        return this;
    }
}
