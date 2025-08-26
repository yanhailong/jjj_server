package com.jjg.game.common.rpc;

import com.jjg.game.common.baselogic.DefaultCallback;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.curator.NodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
    // 服务提供方的节点类型
    private final List<NodeType> providerNodeType = new ArrayList<>();
    /**
     * {@link com.jjg.game.common.constant.CoreConst.GameMajorType}中的值
     */
    private final List<Integer> gameMajorType = new ArrayList<>();
    /**
     * 客户端过滤器,会过滤符合条件的客户端
     */
    private final List<Predicate<ClusterClient>> clientFilter = new ArrayList<>();
    /**
     * 完成所有请求之后的响应回调,此调用，无管是否全部请求都有成功返回或者等待超时后都会调用，所有成功也会调用
     */
    private DefaultCallback allFinishedCallback;
    /**
     * 仅当所有请求都成功返回时才调用
     */
    private DefaultCallback allSuccessCallback;


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

    public List<NodeType> getProviderNodeType() {
        return providerNodeType;
    }

    public List<Integer> getGameMajorType() {
        return gameMajorType;
    }

    public RpcReqParameterBuilder addProviderNodeType(NodeType nodeType) {
        this.providerNodeType.add(nodeType);
        return this;
    }

    public RpcReqParameterBuilder addGameMajorType(int gameMajorType) {
        this.gameMajorType.add(gameMajorType);
        return this;
    }

    public DefaultCallback getAllFinishedCallback() {
        return allFinishedCallback;
    }

    public RpcReqParameterBuilder setAllFinishedCallback(DefaultCallback allFinishedCallback) {
        this.allFinishedCallback = allFinishedCallback;
        return this;
    }

    public DefaultCallback getAllSuccessCallback() {
        return allSuccessCallback;
    }

    public RpcReqParameterBuilder setAllSuccessCallback(DefaultCallback allSuccessCallback) {
        this.allSuccessCallback = allSuccessCallback;
        return this;
    }

    public List<Predicate<ClusterClient>> getClientFilter() {
        return clientFilter;
    }

    public RpcReqParameterBuilder addClientFilter(Predicate<ClusterClient> clientPredicate) {
        clientFilter.add(clientPredicate);
        return this;
    }
}
