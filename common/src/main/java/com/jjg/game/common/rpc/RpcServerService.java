package com.jjg.game.common.rpc;

import cn.hutool.core.convert.BasicType;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterConnect;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterProcessorExecutors;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.rpc.msg.ReqRpcServiceData;
import com.jjg.game.common.rpc.msg.RespRpcServiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RPC服务端服务
 *
 * @author 2CL
 */
@Service
public class RpcServerService {

    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private ClusterRpcService clusterRpcService;
    @Autowired
    private RpcClientService rpcClientService;
    @Autowired
    private ClusterProcessorExecutors processorExecutors;
    // spel
    private final ExpressionParser parser = new SpelExpressionParser();

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 处理rpc请求
     */
    public void reqClusterRpcMessage(ClusterConnect clusterConnect, ReqRpcServiceData req) {
        RespRpcServiceData resp = new RespRpcServiceData();
        if (req == null) {
            throw new RuntimeException("调用RPC时，参数为空");
        }
        log.debug("节点：{} 收到游戏Rpc请求消息:{}", clusterSystem.getNodePath(), req);
        resp.requestId = req.requestId;
        resp.success = false;
        Object provider = clusterRpcService.getProvider(req.serviceClassName);
        // 如果没有找到对应的Provider
        if (provider == null) {
            log.debug("节点：{} 找不到对应的服务提供者: {}", clusterSystem.getNodePath(), req.serviceClassName);
            // 发送返回数据
            clusterConnect.write(new ClusterMessage(resp));
            return;
        }
        // 基础类名
        Map<String, Class<?>> basicName =
            BasicType.PRIMITIVE_WRAPPER_MAP.keySet().stream()
                .collect(HashMap::new, (map, e) -> map.put(e.getName(), e), HashMap::putAll);
        Map<String, Object> parameterNameOfData = JSON.parseObject(req.parameterTypeWithData);
        Class<?>[] parameterTypes = new Class[parameterNameOfData.size()];
        // 数据
        Object[] args = new Object[parameterNameOfData.size()];
        int i = 0;
        try {
            // 解析key
            StandardEvaluationContext context = new StandardEvaluationContext(provider);
            String[] paramsName = new String[parameterNameOfData.size()];
            for (Map.Entry<String, Object> entry : parameterNameOfData.entrySet()) {
                args[i] = entry.getValue();
                // 如果是基础类型
                if (basicName.containsKey(entry.getKey())) {
                    parameterTypes[i] = basicName.get(entry.getKey());
                } else {
                    parameterTypes[i] = Class.forName(entry.getKey());
                }
                if (parameterTypes[i].isAnnotationPresent(Param.class)) {
                    Param param = parameterTypes[i].getAnnotation(Param.class);
                    paramsName[i] = param.value();
                } else {
                    paramsName[i] = "arg[" + i + "]";
                }
                context.setVariable(paramsName[i], args[i]);
                i++;
            }
            Method method = provider.getClass().getMethod(req.serviceMethodName, parameterTypes);
            RpcCallSetting rpcCallSettingAnno = method.getAnnotation(RpcCallSetting.class);
            Integer processorId = Integer.MIN_VALUE;
            if (rpcCallSettingAnno != null) {
                processorId =
                    parser.parseExpression(rpcCallSettingAnno.processorModKey()).getValue(context, Integer.class);
            }
            // 如果需要服务端使用指定的线程执行方法
            if (processorId != null && processorId > 0) {
                BaseFuncProcessor processor = processorExecutors.getProcessorById(processorId);
                processor.executeHandler(new BaseHandler<>() {
                    @Override
                    public void action() throws Exception {
                        invokeMethod(method, provider, args, resp, clusterConnect);
                    }
                });
            } else {
                invokeMethod(method, provider, args, resp, clusterConnect);
            }
        } catch (NoSuchMethodException e) {
            log.error("调用RPC时，未找到类：{} 对应的方法：{}", req.serviceClassName, req.serviceMethodName, e);
        } catch (ClassNotFoundException e) {
            log.error("调用RPC时，通过类型未找到对应的类. {}", e.getMessage(), e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("调用RPC时，发生逻辑异常 类：{} 对应的方法：{}", req.serviceClassName, req.serviceMethodName, e);
        }
    }

    /**
     * 调用方法
     */
    private void invokeMethod(
        Method method, Object provider, Object[] args, RespRpcServiceData resp, ClusterConnect clusterConnect)
        throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        // 调用provider中的方法
        Object o = method.invoke(provider, args);
        // 序列化后返回
        resp.responseData = JSON.toJSONString(o);
        resp.success = true;
        clusterConnect.write(new ClusterMessage(resp));
        log.debug("向发送方：{} 返回调用RPC结果:{}", clusterConnect.address(), resp);
    }

    /**
     * 处理rpc响应
     */
    public void resClusterRpcMessage(RespRpcServiceData res) {
        if (res == null || res.requestId == 0) {
            log.debug("收到RPC返回消息，但是节点：{} 接收的数据为空", clusterSystem.getNodePath());
            return;
        }
        CompletableFuture<RespRpcServiceData> completableFuture =
            rpcClientService.completeCompletableFuture(res.requestId);
        // 按道理不应为空
        if (completableFuture == null) {
            log.debug("节点：{} 找不到对应rpc：{} 的Future", clusterSystem.getNodePath(), res.requestId);
            return;
        }
        log.debug("batchSendRpcMsg 收到id：{} 返回的RPC消息：{}", res.requestId, res.responseData);
        // 收到消息，调用完成
        completableFuture.complete(res);
    }
}
