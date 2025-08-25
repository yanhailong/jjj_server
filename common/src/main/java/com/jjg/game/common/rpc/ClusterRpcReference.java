package com.jjg.game.common.rpc;

import com.jjg.game.common.curator.NodeType;

import java.lang.annotation.*;

/**
 * rpc服务引用，rpc服务最好被调用方是不耗时的操作
 *
 * @author 2CL
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClusterRpcReference {

    /**
     * 服务提供方的节点类型
     */
    NodeType[] providerNodeType();

    /**
     * 超时时间，默认10ms
     */
    int timeoutMillis() default 10;

    /**
     * 重试次数，默认调用时尝试5次
     */
    int tryTimes() default 5;
}
