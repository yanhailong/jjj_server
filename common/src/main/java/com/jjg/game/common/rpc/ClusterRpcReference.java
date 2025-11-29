package com.jjg.game.common.rpc;

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
     * 目标节点的节点路径
     */
    String targetNodePath() default "";

    /**
     * 超时时间，默认100ms
     */
    int timeoutMillis() default 1000;

    /**
     * 重试次数，默认调用时尝试5次
     */
    int tryTimes() default 5;
}
