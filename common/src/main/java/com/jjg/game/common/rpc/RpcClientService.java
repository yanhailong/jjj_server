package com.jjg.game.common.rpc;

import cn.hutool.core.lang.Snowflake;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.rpc.msg.ReqRpcServiceData;
import com.jjg.game.common.rpc.msg.RespRpcServiceData;
import com.jjg.game.common.utils.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * rpc客户端服务
 *
 * @author 2CL
 */
@Service
public class RpcClientService {

    private static final Logger log = LoggerFactory.getLogger(RpcClientService.class);
    @Autowired
    private ClusterSystem clusterSystem;
    // 消息发送中的CompletableFuture合集,发送异常和收到请求对应的响应消息ID时从map中移除
    private final Map<Long, CompletableFuture<RespRpcServiceData>> messagePending = new ConcurrentHashMap<>();
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
            String className, Method method, Object[] args, ClusterRpcReference reference)
            throws TimeoutException, ExecutionException, InterruptedException {

        // 【新增】优先检测本地调用
        Pair<Boolean, Object> pairResult = tryInvokeLocal(className, method, args);
        if (pairResult.getFirst()) {
            return pairResult.getSecond();
        }

        // 原有的远程调用逻辑
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        RpcReqParameterBuilder rpcReqParameter = GameRpcContext.getContext().getReqParameterBuilder();
        List<Pair<String, Object>> parameterArgsMap = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            parameterArgsMap.add(new Pair<>(parameter.getType().getName(), args[i]));
        }
        // 解析节点客户端列表
        List<ClusterClient> clusterClients = parseClusterClients(rpcReqParameter, reference);
        if (clusterClients.isEmpty()) {
            log.warn("调用RPC：{}.{} 未发现对应的节点客户端, 配置：{}", className, methodName, reference);
            return null;
        }
        // RPC服务数据载体消息
        ReqRpcServiceData message = new ReqRpcServiceData();
        message.serviceClassName = className;
        message.serviceMethodName = methodName;
        message.parameterTypeWithData = JSON.toJSONString(parameterArgsMap);

        int waitTime = rpcReqParameter != null && rpcReqParameter.getTryMillisPerClient() > 0
                ? rpcReqParameter.getTryMillisPerClient() : reference.timeoutMillis();
        // TODO.2CL 后续加入重试
        int retryTimes = rpcReqParameter != null && rpcReqParameter.getRetryTimesPerClient() > 0
                ? rpcReqParameter.getRetryTimesPerClient() : reference.tryTimes();

        // 发送消息并等待
        return flushMessageAndWait(clusterClients, message, waitTime, method.getReturnType());
    }

    /**
     * 尝试本地调用(同JVM内直接调用,避免序列化和网络开销)
     *
     * @param className 服务接口类名
     * @param method    方法
     * @param args      参数
     * @return 调用结果,如果本地不存在该服务返回null
     */
    private Pair<Boolean, Object> tryInvokeLocal(String className, Method method, Object[] args) {
        try {
            ApplicationContext context = CommonUtil.getContext();
            if (context == null) {
                return new Pair<>(false, null);
            }

            // 1. 尝试通过接口类型查找实现类
            Class<?> interfaceClass = Class.forName(className);
            Map<String, ?> beans = context.getBeansOfType(interfaceClass);

            if (beans.isEmpty()) {
                // 本地没有该服务实现
                return new Pair<>(false, null);
            }

            // 2. 获取第一个实现类(通常只有一个)
            Object serviceBean = beans.values().iterator().next();

            // 3. 直接本地调用
            Object result = method.invoke(serviceBean, args);

            log.debug("RPC本地调用成功: {}.{}", className, method.getName());
            return new Pair<>(true, result);

        } catch (ClassNotFoundException e) {
            // 接口类不存在,可能是依赖问题,继续走远程调用
            log.debug("本地未找到RPC接口类: {}, 将使用远程调用", className);
            return new Pair<>(false, null);
        } catch (Exception e) {
            // 本地调用失败,记录日志后返回null,让框架继续尝试远程调用
            log.warn("RPC本地调用失败: {}.{}, 将回退到远程调用, error: {}",
                    className, method.getName(), e.getMessage());
            return new Pair<>(false, null);
        }
    }

    /**
     * 解析客户端列表
     */
    private List<ClusterClient> parseClusterClients(
            RpcReqParameterBuilder rpcReqParameter, ClusterRpcReference reference) {
        // 优先使用请求参数中的节点
        List<ClusterClient> clusterClients = new ArrayList<>();
        String nodePath = reference.targetNodePath();
        if (!StringUtils.isEmpty(nodePath)) {
            ClusterClient client = clusterSystem.getClusterByPath(nodePath);
            if (client != null) {
                clusterClients.add(client);
            }
        }
        // 使用RpcContext中的请求参数
        if (rpcReqParameter != null) {
            if (!rpcReqParameter.getClusterClients().isEmpty()) {
                clusterClients =
                        new ArrayList<>(rpcReqParameter.getClusterClients().stream().filter(Objects::nonNull).toList());
            }
            // 如果没有传节点，则向默认节点类型中的所有节点发送
            if (clusterClients.isEmpty()) {
                for (NodeType nodeType : rpcReqParameter.getProviderNodeType()) {
                    List<ClusterClient> nodeClusterClient = clusterSystem.getNodesByType(nodeType);
                    clusterClients.addAll(nodeClusterClient);
                }
                if (!rpcReqParameter.getGameMajorType().isEmpty()) {
                    List<ClusterClient> filteredClusterClient = new ArrayList<>();
                    for (int gameMajorType : rpcReqParameter.getGameMajorType()) {
                        clusterClients.forEach(clusterClient -> {
                            if (clusterClient.nodeConfig.inMajorType(gameMajorType)) {
                                filteredClusterClient.add(clusterClient);
                            }
                        });
                    }
                    clusterClients = filteredClusterClient;
                }
            }
            // 参数中指定条件过滤
            if (!clusterClients.isEmpty() && !rpcReqParameter.getClientFilter().isEmpty()) {
                for (Predicate<ClusterClient> predicate : rpcReqParameter.getClientFilter()) {
                    clusterClients.removeIf(predicate);
                }
            }
        }
        return clusterClients.stream().filter(Objects::nonNull).toList();
    }

    /**
     * 发送并等待消息回传
     *
     * @param clusterClients 节点客户端
     * @param message        消息
     * @param waitTime       等待时间
     * @param returnType     返回类型
     */
    private Object flushMessageAndWait(
            List<ClusterClient> clusterClients, ReqRpcServiceData message, int waitTime, Class<?> returnType)
            throws TimeoutException, ExecutionException, InterruptedException {
        if (clusterClients.size() == 1) {
            return dealSingleClientRpcMsg(clusterClients.getFirst(), message, waitTime, returnType);
        } else {
            return batchSendRpcMsg(clusterClients, message, waitTime, returnType);
        }
    }

    /**
     * 处理单个客户端消息
     */
    private Object dealSingleClientRpcMsg(
            ClusterClient clusterClient, ReqRpcServiceData message, int waitTime, Class<?> returnType)
            throws TimeoutException, ExecutionException, InterruptedException {
        Object responseData = null;
        String nodePath = clusterClient.marsNode.getNodePath();
        CompletableFuture<RespRpcServiceData> completableFuture = new CompletableFuture<>();
        // 请求ID
        long requestId = message.requestId = requestIdGenerator.nextId();
        messagePending.put(requestId, completableFuture);
        log.debug("SingleClientRpcMsg put ID, {}", requestId);
        try {
            clusterClient.getConnect().writeWithFuture(new ClusterMessage(message), f -> {
                if (!f.isSuccess()) {
                    CompletableFuture<?> finishedFuture = messagePending.remove(requestId);
                    log.error("SingleClientRpcMsg 调用RPC向节点:{} 发送消息异常!", nodePath);
                    finishedFuture.completeExceptionally(f.cause());
                }
            });
        } catch (InterruptedException exception) {
            log.error("SingleClientRpcMsg 调用RPC向节点:{} 发送消息异常：{}", nodePath, exception.getMessage(), exception);
            throw exception;
        }
        try {
            // 返回消息
            RespRpcServiceData responseObject =
                    completableFuture.get(waitTime, TimeUnit.MILLISECONDS);
            log.debug("SingleClientRpcMsg 收到节点：{} 返回的RPC消息：{}", nodePath, responseObject);
            // 要有返回值
            if (!Void.TYPE.isAssignableFrom(returnType) && responseObject != null) {
                if (responseObject.responseData != null && responseObject.success) {
                    responseData = JSON.parseObject(responseObject.responseData, returnType);
                }
            }
        } catch (InterruptedException | ExecutionException exception) {
            log.warn("SingleClientRpcMsg 调用RPC向节点：{} 发出消息：{} 异常", nodePath, message, exception);
            throw exception;
        } catch (TimeoutException exception) {
            log.warn("SingleClientRpcMsg 调用RPC向节点：{} 发出消息：{} 超时", nodePath, message);
            throw exception;
        }
        GameRpcContext rpcMetadataCarrier = GameRpcContext.getContext();
        if (rpcMetadataCarrier != null) {
            Map<String, Object> respDataMap = new HashMap<>();
            respDataMap.put(clusterClient.nodeConfig.getName(), responseData);
            rpcMetadataCarrier.setDataOfNode(respDataMap);
            rpcMetadataCarrier.setDataList(Collections.singletonList(responseData));
        }
        return responseData;
    }

    /**
     * 批量给指定的客户端发送RPC消息
     */
    private Object batchSendRpcMsg(
            List<ClusterClient> clusterClients, ReqRpcServiceData message, int waitTime, Class<?> returnType) throws InterruptedException {
        Map<String, Object> responseMap = new ConcurrentHashMap<>();
        CountDownLatch countDownLatch = new CountDownLatch(clusterClients.size());
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            clusterClients.forEach(clusterClient -> virtualThreadExecutor.submit(() -> {
                String nodePath = clusterClient.marsNode.getNodePath();
                CompletableFuture<RespRpcServiceData> completableFuture = new CompletableFuture<>();
                // 请求ID
                long requestId = message.requestId = requestIdGenerator.nextId();
                messagePending.put(requestId, completableFuture);
                log.debug("batchSendRpcMsg send cluster: {} with id, {}",
                        clusterClient.nodeConfig.getName(), requestId);
                try {
                    clusterClient.getConnect().writeWithFuture(new ClusterMessage(message), f -> {
                        if (!f.isSuccess()) {
                            CompletableFuture<?> finishedFuture = messagePending.remove(requestId);
                            log.error("batchSendRpcMsg 调用RPC向节点:{} 发送消息异常!", nodePath);
                            finishedFuture.completeExceptionally(f.cause());
                        }
                    });
                } catch (InterruptedException e) {
                    log.error("batchSendRpcMsg 调用RPC向节点:{} 发送消息异常：{}", nodePath, e.getMessage(), e);
                    responseMap.put(nodePath, null);
                }
                try {
                    // 返回消息
                    RespRpcServiceData responseObject =
                            completableFuture.get(waitTime, TimeUnit.MILLISECONDS);
                    boolean success = Void.TYPE.isAssignableFrom(returnType);
                    // 要有返回值
                    if (!success && responseObject != null) {
                        if (responseObject.responseData != null && responseObject.success) {
                            Object returnData = JSON.parseObject(responseObject.responseData, returnType);
                            responseMap.put(nodePath, returnData);
                            success = true;
                        }
                    }
                    if (success) {
                        // 成功请求才countdown
                        countDownLatch.countDown();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("batchSendRpcMsg 调用RPC向节点：{} 发出消息：{} 异常", nodePath, message, e);
                    responseMap.put(nodePath, null);
                } catch (TimeoutException e) {
                    log.warn("batchSendRpcMsg 调用RPC向节点：{} 发出消息：{} 超时", nodePath, message);
                    responseMap.put(nodePath, null);
                }
            }));
        }
        try {
            int maxWaitTime = waitTime * clusterClients.size();
            GameRpcContext gameRpcContext = GameRpcContext.getContext();
            if (countDownLatch.await(maxWaitTime, TimeUnit.MILLISECONDS)) {
                log.debug("batchSendRpcMsg 给节点：{} 发送请求全部返回成功",
                        clusterClients.stream().map(c -> c.nodeConfig.getName()).collect(Collectors.joining(",")));
                if (gameRpcContext != null) {
                    // 所有请求都成功后调用
                    gameRpcContext.getReqParameterBuilder().getAllSuccessCallback().run();
                }
            } else {
                log.warn("batchSendRpcMsg 给节点：{} 发送请求结束, 成功数量：{}",
                        clusterClients.stream().map(c -> c.nodeConfig.getName()).collect(Collectors.joining(",")),
                        (clusterClients.size() - countDownLatch.getCount()));
            }
            if (gameRpcContext != null) {
                gameRpcContext.setDataOfNode(responseMap);
                gameRpcContext.setDataList(responseMap.values().stream().toList());
                // 所有完成，call回调
                gameRpcContext.getReqParameterBuilder().getAllFinishedCallback().run();
            }
            return responseMap.values().stream().findFirst().orElse(null);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 通过请求ID获取future
     */
    public CompletableFuture<RespRpcServiceData> completeCompletableFuture(long requestId) {
        return messagePending.remove(requestId);
    }
}