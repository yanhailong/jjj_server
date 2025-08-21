package com.jjg.game.common.redis;

/**
 * 加锁失败时的策略
 *
 * @author 2CL
 */
public enum LockFailedStrategy {
    // 抛异常
    EXCEPTION,
    // 执行对应的回调
    CALLBACK,
    // 不执行任何操作
    NONE,
}
