package com.jjg.game.common.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 游戏RPC上下文
 *
 * @author 2CL
 */
public class GameRpcContext {

    /**
     * rpc构建的上下文,线程依赖,无论如何都需要保证在RPC调用完成后将当前参数引用是设置为空{@link #clearRpcBuilderData}
     */
    private static final ThreadLocal<GameRpcContext> METADATA_CARRIER_THREAD_LOCAL = new ThreadLocal<>();
    private static final Logger log = LoggerFactory.getLogger(GameRpcContext.class);

    // 每个节点的数据
    private Map<String, Object> dataOfNode;
    // 多个节点下数据列表
    private List<Object> dataList;
    // 请求参数
    private RpcReqParameterBuilder reqParameterBuilder;

    public GameRpcContext() {
    }

    public GameRpcContext(RpcReqParameterBuilder reqParameterBuilder) {
        this.reqParameterBuilder = reqParameterBuilder;
    }

    public Map<String, Object> getDataOfNode() {
        return dataOfNode;
    }

    public void setDataOfNode(Map<String, Object> dataOfNode) {
        this.dataOfNode = dataOfNode;
    }

    public List<Object> getDataList() {
        return dataList;
    }

    public void setDataList(List<Object> dataList) {
        this.dataList = dataList;
    }

    public RpcReqParameterBuilder getReqParameterBuilder() {
        return reqParameterBuilder;
    }

    public GameRpcContext withReqParameterBuilder(RpcReqParameterBuilder reqParameterBuilder) {
        setReqParameterBuilder(reqParameterBuilder);
        return this;
    }

    public void setReqParameterBuilder(RpcReqParameterBuilder reqParameterBuilder) {
        this.reqParameterBuilder = reqParameterBuilder;
    }

    public void reset() {
        dataList = null;
        dataOfNode = null;
        this.reqParameterBuilder = null;
    }

    public void clearRpcBuilderData() {
        METADATA_CARRIER_THREAD_LOCAL.get().reset();
        setReqParameterBuilder(null);
    }

    /**
     * 异步请求
     */
    public <T> CompletableFuture<T> asyncCall(Callable<T> callable) {
        RpcReqParameterBuilder rpcReqParameterBuilder = GameRpcContext.getContext().getReqParameterBuilder();
        return CompletableFuture.supplyAsync(() -> {
            GameRpcContext.getContext().setReqParameterBuilder(rpcReqParameterBuilder);
            try {
                return callable.call();
            } catch (Exception e) {
                log.error("调用发生异常 {}", e.getMessage(), e);
                throw new RuntimeException(e);
            } finally {
                GameRpcContext.getContext().clearRpcBuilderData();
            }
        });
    }

    public static GameRpcContext getContext() {
        if (METADATA_CARRIER_THREAD_LOCAL.get() == null) {
            METADATA_CARRIER_THREAD_LOCAL.set(new GameRpcContext());
        }
        return METADATA_CARRIER_THREAD_LOCAL.get();
    }
}
