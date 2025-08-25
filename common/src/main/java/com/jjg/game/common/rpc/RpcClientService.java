package com.jjg.game.common.rpc;

import cn.hutool.core.lang.Snowflake;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.message.RpcServiceDataCarrierMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 *
 * @author 2CL
 */
@Service
public class RpcClientService {

    private static final Logger log = LoggerFactory.getLogger(RpcClientService.class);
    @Autowired
    private ClusterSystem clusterSystem;
    // 消息发送中的CompletableFuture合集
    private final Map<Long, CompletableFuture<RpcServiceDataCarrierMessage>> messagePending = new ConcurrentHashMap<>();
    // 消息ID生成器
    private final Snowflake requestIdGenerator = new Snowflake(10, 10);

    /**
     * 尝试调用远程服务
     *
     * @param className 目标类名
     * @param method    方法
     * @param args      参数数据
     * @param reference rpc引用注解，超时、尝试次数...
     * @return 调用返回值
     */
    public Object tryInvokeRemote(
        String className, Method method, Object[] args, ClusterRpcReference reference) {
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        RpcReqParameterBuilder rpcReqParameter = null;
        Map<Parameter, Object> parameterArgsMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getType().equals(RpcReqParameterBuilder.class)) {
                rpcReqParameter = (RpcReqParameterBuilder) args[i];
                continue;
            }
            parameterArgsMap.put(parameters[i], args[i]);
        }
        // 优先使用请求参数中的节点
        List<ClusterClient> clusterClients = new ArrayList<>();
        if (rpcReqParameter != null && !rpcReqParameter.getClusterClients().isEmpty()) {
            clusterClients = rpcReqParameter.getClusterClients();
        }
        // 如果没有传节点，则向默认节点类型中的所有节点发送
        if (clusterClients.isEmpty()) {
            for (NodeType nodeType : reference.providerNodeType()) {
                List<ClusterClient> nodeClusterClient = clusterSystem.getNodesByType(nodeType);
                clusterClients.addAll(nodeClusterClient);
            }
        }
        if (clusterClients.isEmpty()) {
            log.warn("调用RPC：{}.{} 未发现对应的节点客户端, 配置：{}", className, methodName, reference);
            return null;
        }
        // RPC服务数据载体消息
        RpcServiceDataCarrierMessage message = new RpcServiceDataCarrierMessage();
        message.requestId = requestIdGenerator.nextId();
        message.serviceClassName = className;
        message.serviceMethodName = methodName;
        message.parameterTypeWithData = JSON.toJSONString(parameterArgsMap);

        int waitTime = rpcReqParameter != null && rpcReqParameter.getTryMillisPerClient() != 0
            ? rpcReqParameter.getTryMillisPerClient() : reference.timeoutMillis();

        int retryTimes = rpcReqParameter != null && rpcReqParameter.getRetryTimesPerClient() != 0
            ? rpcReqParameter.getRetryTimesPerClient() : reference.tryTimes();

        // 发送消息并等待
        return flushMessageAndWait(clusterClients, message, waitTime, method.getReturnType());
    }

    /**
     * 发送并等待消息回传
     *
     * @param clusterClients 节点客户端
     * @param message        消息
     * @param waitTime       等待时间
     * @param returnType     返回类型
     */
    private Map<String, Object> flushMessageAndWait(
        List<ClusterClient> clusterClients, RpcServiceDataCarrierMessage message, int waitTime, Class<?> returnType) {
        Map<String, Object> responseMap = new ConcurrentHashMap<>();
        // 计数器
        CountDownLatch countDownLatch = new CountDownLatch(clusterClients.size());
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            clusterClients.forEach(clusterClient -> virtualThreadExecutor.submit(() -> {
                String nodePath = clusterClient.marsNode.getNodePath();
                CompletableFuture<RpcServiceDataCarrierMessage> completableFuture = new CompletableFuture<>();
                // 请求ID
                long requestId = message.requestId;
                messagePending.put(requestId, completableFuture);
                try {
                    clusterClient.getConnect().writeWithFuture(message, f -> {
                        CompletableFuture<?> finishedFuture = messagePending.remove(requestId);
                        if (!f.isSuccess()) {
                            finishedFuture.completeExceptionally(f.cause());
                        }
                    });
                } catch (InterruptedException e) {
                    log.error("调用RPC向节点:{} 发送消息异常：{}", nodePath, e.getMessage(), e);
                    responseMap.put(nodePath, null);
                }
                try {
                    // 返回消息
                    RpcServiceDataCarrierMessage responseObject =
                        completableFuture.get(waitTime, TimeUnit.MILLISECONDS);
                    // 要有返回值
                    if (!Void.TYPE.isAssignableFrom(returnType)) {
                        if (responseObject != null && responseObject.responseData != null) {
                            Object returnData = JSON.parseObject(responseObject.responseData, returnType);
                            responseMap.put(nodePath, returnData);
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.warn("调用RPC向节点：{} 发生消息：{} 超时", nodePath, message);
                    responseMap.put(nodePath, null);
                } finally {
                    countDownLatch.countDown();
                }
            }));
        }
        try {
            // 等待一定时间，等待所有消息到达，否则放弃
            countDownLatch.wait((long) waitTime * clusterClients.size());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return responseMap;
    }

    /**
     * 通过请求ID获取future
     */
    public CompletableFuture<RpcServiceDataCarrierMessage> getCompletableFuture(long requestId) {
        return messagePending.get(requestId);
    }
}
