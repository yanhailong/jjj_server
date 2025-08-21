package com.jjg.game.common.redis;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * redisson锁，作用于需要对整个方法内的数据操作逻辑进行加锁的情形，支持重入
 *
 * @author 2CL
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedissonLock {

    /**
     * 锁key,支持SpringEL表达式
     */
    String key();

    /**
     * 锁key,支持SpringEL表达式,支持多个key进行组合加锁,联锁
     */
    String[] keys() default {};

    /**
     * 锁超时释放时间leaseTime 30s
     */
    long leaseTime() default 30_000;

    /**
     * 等待时间,tryLock等待的最大时长
     */
    long waitTime() default 100;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 降级策略
     */
    LockFailedStrategy failedStrategy() default LockFailedStrategy.EXCEPTION;

    /**
     * 当失败策略为CALLBACK时,指定回调的方法,只能调用注解方法内的函数
     */
    String fallbackMethod() default "";
}
