package com.jjg.game.common.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * rpc客户端方法调用代理
 *
 * @author 2CL
 */
public class RpcClientProxy {

    /**
     * 代理rpc接口
     *
     * @param clazz               rpc接口类
     * @param clientService       client服务
     * @param clusterRpcReference rpc引用注解
     * @param <T>                 t
     * @return t
     */
    public <T> T proxyRpcInterface(
        Class<T> clazz, RpcClientService clientService, ClusterRpcReference clusterRpcReference) {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (proxy == Object.class) {
                return method.invoke(this, args);
            }
            // 尝试调用远程类的方法
            return clientService.tryInvokeRemote(clazz.getName(), method, args, clusterRpcReference);
        };
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, invocationHandler);
    }
}
