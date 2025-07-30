package com.jjg.game.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁, TODO 后续优化成注解的方式
 *
 * @author 11
 * @date 2025/5/26 9:44
 */
@Component
public class RedisLock {
    private static final String tableName = "lock:";

    @Autowired
    private RedisTemplate redisTemplate;

    private final ThreadLocal<String> lockId = new ThreadLocal<>();

    private String getKey(String key) {
        return tableName + key;
    }

    public boolean tryLock(String key) {
        String uuid = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, uuid, 100, TimeUnit.MILLISECONDS);
        if (Boolean.TRUE.equals(success)) {
            lockId.set(uuid);
            return true;
        }
        return false;
    }

    public void tryUnlock(String key) {
        String uuid = lockId.get();
        if (uuid == null) {
            // 如果其他线程切入时会出现此情况
            return;
        }

        String lua =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "   return redis.call('del', KEYS[1]) " +
                "else return 0 end";

        RedisScript<Long> script = RedisScript.of(lua, Long.class);
        redisTemplate.execute(script, Collections.singletonList(key), uuid);
        lockId.remove();
    }

    /**
     * FIXME 所有线程都设置相同值？，当一个线程释放锁时可能把另一个线程的锁删掉（如果加锁后某线程被挂起，另一个线程抢占锁）
     */
    public boolean lock(String key) {
        return redisTemplate.opsForValue().setIfAbsent(getKey(key), 1, 100, TimeUnit.MILLISECONDS);
    }

    public boolean lock(String key, long expireTime) {
        return redisTemplate.opsForValue().setIfAbsent(getKey(key), 1, expireTime, TimeUnit.MILLISECONDS);
    }

    public boolean lock(String key, long expireTime, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(getKey(key), 1, expireTime, timeUnit);
    }

    /**
     * FIXME 不是原子性操作，不能保证 get + delete 是同一个线程执行的，会有并发风险
     *
     * @param key
     */
    public void unlock(String key) {
        redisTemplate.delete(getKey(key));
    }
}
