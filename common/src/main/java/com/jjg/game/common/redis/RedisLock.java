package com.jjg.game.common.redis;

import org.redisson.RedissonLock;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁, TODO 后续优化添加注解加锁和释放锁的方式
 *
 * @author 11
 * @date 2025/5/26 9:44
 */
@Component
public class RedisLock {

    private static final String LOCK_TABLE_NAME = "RDlock:";

    @Autowired
    private RedissonClient redissonClient;

    private String getKey(String key) {
        return LOCK_TABLE_NAME + key;
    }

    /**
     * 尝试获取锁，适用于尝试性任务，不获取锁也能继续
     *
     * @param key 锁名
     * @return 是否加锁成功
     */
    public boolean tryLock(String key) {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        return redissonLock.tryLock();
    }

    /**
     * 尝试获取锁，默认使用毫秒为计数单位，适用于尝试性任务，不获取锁也能继续
     *
     * @param key      锁名
     * @param waitTime 尝试等待获取时间
     * @return 是否成功
     * @throws InterruptedException e
     */
    public boolean tryLock(String key, long waitTime) throws InterruptedException {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        return redissonLock.tryLock(waitTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试获取锁，适用于尝试性任务，不获取锁也能继续
     *
     * @param key      锁名
     * @param waitTime 尝试等待获取时间
     * @param timeUnit 时间格式
     * @return 是否成功
     * @throws InterruptedException e
     */
    public boolean tryLock(String key, long waitTime, TimeUnit timeUnit) throws InterruptedException {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        return redissonLock.tryLock(waitTime, timeUnit);
    }


    /**
     * 尝试获取锁，适用于尝试性任务，不获取锁也能继续
     *
     * @param key       锁名
     * @param waitTime  尝试等待获取时间
     * @param leaseTime 超时锁释放时间
     * @param timeUnit  时间格式
     * @return 是否成功
     * @throws InterruptedException e
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        return redissonLock.tryLock(waitTime, leaseTime, timeUnit);
    }

    /**
     * 释放锁
     */
    public void tryUnlock(String key) {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        redissonLock.unlock();
    }


    /**
     * 必须需要获取到锁的业务调用此方法，强制等待直到获取到锁，默认等待30s
     */
    public void lock(String key) {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        redissonLock.lock();
    }

    /**
     * 带过期时间的加锁 必须需要获取到锁的业务调用此方法，强制等待直到获取到锁，默认等待waitTime s
     *
     * @param key      锁名
     * @param waitTime 等待时间
     */
    public void lock(String key, int waitTime) {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        redissonLock.lock(waitTime, TimeUnit.SECONDS);
    }

    /**
     * 加锁 必须需要获取到锁的业务调用此方法，强制等待直到获取到锁，默认等待 waitTime
     *
     * @param key       锁名
     * @param leaseTime 等待时间
     */
    public void lock(String key, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        redissonLock.lock(leaseTime, timeUnit);
    }

    /**
     * 获取读锁 并发读
     *
     * @param keyName 锁名
     */
    public RLock getReadLock(String keyName) {
        RReadWriteLock rReadWriteLock = redissonClient.getReadWriteLock(getKey(keyName));
        RLock rLock = rReadWriteLock.readLock();
        rLock.lock();
        return rLock;
    }


    /**
     * 获取写锁 独占写
     *
     * @param keyName 锁名
     */
    public RLock getWriteLock(String keyName, long writeWaitTime) throws InterruptedException {
        RReadWriteLock rReadWriteLock = redissonClient.getReadWriteLock(getKey(keyName));
        RLock rLock = rReadWriteLock.writeLock();
        rLock.tryLock(writeWaitTime, TimeUnit.MILLISECONDS);
        return rLock;
    }

    /**
     * 释放读写锁
     */
    public void unlockReadWriteLock(RLock rLock) {
        if (rLock != null && rLock.isHeldByCurrentThread()) {
            rLock.forceUnlock();
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        redissonLock.unlock();
    }

    /**
     * 强制释放锁
     */
    public void forceUnlock(String key) {
        RedissonLock redissonLock = (RedissonLock) redissonClient.getLock(getKey(key));
        redissonLock.forceUnlock();
    }
}
