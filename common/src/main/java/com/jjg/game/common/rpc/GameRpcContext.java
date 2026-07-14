package com.jjg.game.common.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    /**
     * 异步 RPC 专用有界线程池: callable 内部是阻塞式等待响应(最长 RPC 超时),
     * 不能用 ForkJoinPool.commonPool(4 核机并行度仅 3, 对端变慢时全部阻塞且任务无界堆积)。
     * 队列满时快速拒绝, 由调用方按失败处理(异步联动本身允许失败)。
     */
    private static final ExecutorService ASYNC_CALL_EXECUTOR = createAsyncCallExecutor();

    private static ExecutorService createAsyncCallExecutor() {
        int threads = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
        AtomicInteger seq = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threads, threads,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024),
                r -> {
                    Thread t = new Thread(r, "rpc-async-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                });
        //空闲线程可回收, 不常驻
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

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
        reset();
        setReqParameterBuilder(null);
    }

    /**
     * 异步请求 TODO.2CL RPC异步回调时需要注意线程安全问题
     */
    public <T> CompletableFuture<T> asyncCall(Callable<T> callable) {
        // 需要将外部参数进行传递
        RpcReqParameterBuilder rpcReqParameterBuilder = GameRpcContext.getContext().getReqParameterBuilder();
        try {
            return CompletableFuture.supplyAsync(() -> {
                GameRpcContext.getContext().setReqParameterBuilder(rpcReqParameterBuilder);
                try {
                    return callable.call();
                } catch (Exception e) {
                    log.error("异步调用rpc发生异常 {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                } finally {
                    GameRpcContext.getContext().clearRpcBuilderData();
                }
            }, ASYNC_CALL_EXECUTOR);
        } catch (RejectedExecutionException e) {
            log.warn("异步RPC线程池已满, 放弃本次调用");
            return CompletableFuture.failedFuture(e);
        }
    }

    public static GameRpcContext getContext() {
        if (METADATA_CARRIER_THREAD_LOCAL.get() == null) {
            METADATA_CARRIER_THREAD_LOCAL.set(new GameRpcContext());
        }
        return METADATA_CARRIER_THREAD_LOCAL.get();
    }
}
