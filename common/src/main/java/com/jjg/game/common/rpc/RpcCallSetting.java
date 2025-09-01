package com.jjg.game.common.rpc;

import java.lang.annotation.*;

/**
 * rpc执行参数,针对每个方法进行设置,例如：指定到某个线程，然后调用方法
 *
 * @author 2CL
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcCallSetting {

    /**
     * 通过此key，获取调用方法中，注解了{@link org.springframework.data.repository.query.Param}的参数
     * 支持springEL 表达式
     * @return processor id key
     */
    String processorModKey();
}
