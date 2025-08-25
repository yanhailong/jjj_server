package com.jjg.game.common.rpc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 拦截ClusterRpcReference注解
 *
 * @author 2CL
 */
@Component
public class ClusterRpcPostProcessor implements BeanPostProcessor {

    @Autowired
    private RpcClientService rpcClientService;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
        throws BeansException, IllegalArgumentException {
        for (Field declaredField : bean.getClass().getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(ClusterRpcReference.class)) {
                ClusterRpcReference annotation = declaredField.getAnnotation(ClusterRpcReference.class);
                RpcClientProxy rpcClientProxy = new RpcClientProxy();
                Object proxiedRpcReference =
                    rpcClientProxy.proxyRpcInterface(declaredField.getType(), rpcClientService, annotation);
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, proxiedRpcReference);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }
}
