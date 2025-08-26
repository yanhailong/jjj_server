package com.jjg.game.common.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ClusterRpcPostProcessor.class);
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
                    log.info("代理bean: {} 中的字段：{} 为RPC调用引用", bean.getClass().getName(), declaredField.getName());
                    declaredField.set(bean, proxiedRpcReference);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }
}
