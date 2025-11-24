package com.jjg.game.common.redis;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * redis分布式锁
 *
 * @author 2CL
 * @date 2025/5/26 9:44
 */
@Component
public class RedisLock {

    private static final String LOCK_TABLE_NAME = "RDlock:";
    private final Logger log = LoggerFactory.getLogger(RedisLock.class);

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
        RLock redissonLock = redissonClient.getLock(getKey(key));
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
        if (isIllegalWaitTime(key, waitTime, TimeUnit.MILLISECONDS)) {
            return false;
        }
        RLock redissonLock = redissonClient.getLock(getKey(key));
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
        if (isIllegalWaitTime(key, waitTime, timeUnit)) {
            return false;
        }
        RLock redissonLock = redissonClient.getLock(getKey(key));
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
        RLock redissonLock = redissonClient.getLock(getKey(key));
        return redissonLock.tryLock(waitTime, leaseTime, timeUnit);
    }

    /**
     * 释放锁
     */
    public void tryUnlock(String key) {
        RLock redissonLock = redissonClient.getLock(getKey(key));
        redissonLock.unlock();
    }


    /**
     * 必须需要获取到锁的业务调用此方法，强制等待直到获取到锁，默认等待30s
     */
    public void lock(String key) {
        RLock redissonLock = redissonClient.getLock(getKey(key));
        redissonLock.lock();
    }

    /**
     * 带过期时间的加锁 必须需要获取到锁的业务调用此方法，强制等待直到获取到锁，默认等待waitTime s
     *
     * @param key      锁名
     * @param waitTime 等待时间
     */
    public void lock(String key, int waitTime) {
        if (isIllegalWaitTime(key, waitTime, TimeUnit.MILLISECONDS)) {
            return;
        }
        RLock redissonLock = redissonClient.getLock(getKey(key));
        redissonLock.lock(waitTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 加锁 必须需要获取到锁的业务调用此方法，强制等待直到获取到锁，默认等待 waitTime
     *
     * @param key       锁名
     * @param leaseTime 等待时间
     */
    public void lock(String key, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        if (isIllegalWaitTime(key, leaseTime, timeUnit)) {
            return;
        }
        RLock redissonLock = redissonClient.getLock(getKey(key));
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
        if (isIllegalWaitTime(keyName, writeWaitTime, TimeUnit.MILLISECONDS)) {
            return null;
        }
        RReadWriteLock rReadWriteLock = redissonClient.getReadWriteLock(getKey(keyName));
        RLock rLock = rReadWriteLock.writeLock();
        boolean lock = rLock.tryLock(writeWaitTime, TimeUnit.MILLISECONDS);
        return lock ? rLock : null;
    }

    /**
     * 检查等待时间是否正确,
     * 建议将尝试获取锁的时间设置至少5ms以上，避免出现  attempt to unlock lock, not locked by current thread 错误
     *
     * @param key      待检查的键
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @return 返回结果
     */
    private boolean isIllegalWaitTime(String key, long waitTime, TimeUnit timeUnit) {
        if (waitTime <= 0) {
            log.warn("尝试加锁或解锁时，等待时间为 0，key: {}", key);
            return true;
        }
        if (!timeUnit.equals(TimeUnit.MILLISECONDS) && !timeUnit.equals(TimeUnit.MICROSECONDS)) {
            return false;
        }
        int leastMillisTime = 5, leastMircoTime = 5_000;
        if (timeUnit.equals(TimeUnit.MILLISECONDS) && waitTime > leastMillisTime) {
            return false;
        }
        if (timeUnit.equals(TimeUnit.MICROSECONDS) && waitTime > leastMircoTime) {
            return false;
        }
        log.warn("加锁：{} 时尝试时间过短！ waitTime: {} timeUnit: {}", key, waitTime, timeUnit);
        return true;
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
        RLock redissonLock = redissonClient.getLock(getKey(key));
        if (redissonLock.isHeldByCurrentThread()) {
            try {
                redissonLock.unlock();
            } catch (Exception e) {
                log.error("unlock redissonLock exception key:{}", key, e);
            }
        }
    }

    /**
     * 强制释放锁
     */
    public void forceUnlock(String key) {
        RLock redissonLock = redissonClient.getLock(getKey(key));
        redissonLock.forceUnlock();
    }

    /**
     * 尝试获取锁并执行逻辑，无返回值
     * 如果获取到锁则执行传入的逻辑代码，执行完毕后自动释放锁
     * 如果没有获取到锁则不执行任何逻辑
     *
     * @param key      锁名
     * @param runnable 要执行的逻辑代码
     */
    public void tryLockAndRun(String key, Runnable runnable) {
        boolean lockAcquired = tryLock(key);
        if (lockAcquired) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("tryLockAndRun error", e);
            } finally {
                unlock(key);
            }
        }
    }

    /**
     * 尝试获取锁并执行逻辑，有返回值
     * 如果获取到锁则执行传入的逻辑代码并返回结果，执行完毕后自动释放锁
     * 如果没有获取到锁则返回null
     *
     * @param key      锁名
     * @param supplier 要执行的逻辑代码
     * @param <T>      返回值类型
     * @return 执行结果，如果未获取到锁则返回null
     */
    public <T> T tryLockAndGet(String key, Supplier<T> supplier) {
        boolean lockAcquired = tryLock(key);
        if (lockAcquired) {
            try {
                return supplier.get();
            } catch (Exception e) {
                log.error("tryLockAndGet error", e);
            } finally {
                unlock(key);
            }
        }
        return null;
    }

    /**
     * 尝试获取锁并执行逻辑，有返回值，可指定默认值
     * 如果获取到锁则执行传入的逻辑代码并返回结果，执行完毕后自动释放锁
     * 如果没有获取到锁则返回指定的默认值
     *
     * @param key          锁名
     * @param supplier     要执行的逻辑代码
     * @param defaultValue 未获取到锁时的默认返回值
     * @param <T>          返回值类型
     * @return 执行结果，如果未获取到锁则返回默认值
     */
    public <T> T tryLockAndGet(String key, Supplier<T> supplier, T defaultValue) {
        boolean lockAcquired = tryLock(key);
        if (lockAcquired) {
            try {
                return supplier.get();
            } catch (Exception e) {
                log.error("tryLockAndGet error", e);
            } finally {
                unlock(key);
            }
        }
        return defaultValue;
    }

    /**
     * 获取锁并执行逻辑代码，执行完毕后自动释放锁。
     * 此方法会尝试获取锁并强制等待直到获取成功，在获取锁后执行传入的逻辑代码，完成后自动释放锁。
     *
     * @param key      锁的唯一标识。
     * @param waitTime 等待锁的时间，单位为秒。
     * @param runnable 需要执行的逻辑代码。
     */
    public void lockAndRun(String key, int waitTime, Runnable runnable) {
        lock(key, waitTime);
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("lockAndRun error", e);
        } finally {
            unlock(key);
        }
    }

    /**
     * 获取锁并执行逻辑代码，执行完毕后自动释放锁。
     * 此方法会尝试获取锁并强制等待直到获取成功，在获取锁后执行传入的逻辑代码并返回结果，完成后自动释放锁。
     *
     * @param <T>      返回值类型
     * @param key      锁的唯一标识。
     * @param waitTime 等待锁的时间，单位为秒。
     * @param supplier 需要执行的逻辑代码，返回一个结果。
     * @return 执行逻辑代码后的返回结果。
     */
    public <T> T lockAndGet(String key, int waitTime, Supplier<T> supplier) {
        lock(key, waitTime);
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("lockAndGet error", e);
            return null;
        } finally {
            unlock(key);
        }
    }

    /**
     * 执行带锁的逻辑（自动释放）
     *
     * @param key      锁键
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @param action   业务逻辑
     * @param <T>      返回值类型
     * @return 业务结果（失败返回 null）
     */
    public <T> T executeWithLock(String key, long waitTime, TimeUnit timeUnit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(getKey(key));
        try {
            if (lock.tryLock(waitTime, timeUnit)) {
                try {
                    return action.get();
                } finally {
                    lock.unlock();
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
