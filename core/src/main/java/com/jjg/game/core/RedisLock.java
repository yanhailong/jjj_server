package com.jjg.game.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁
 * @author 11
 * @date 2025/5/26 9:44
 */
@Component
public class RedisLock {
    private static final String tableName = "lock:";

    @Autowired
    private RedisTemplate redisTemplate;

    private String getKey(String key){
        return tableName + key;
    }

    public boolean lock(String key){
        return redisTemplate.opsForValue().setIfAbsent(getKey(key),1,100, TimeUnit.MILLISECONDS);
    }

    public boolean lock(String key,long expireTime){
        return redisTemplate.opsForValue().setIfAbsent(getKey(key),1,expireTime, TimeUnit.MILLISECONDS);
    }

    public boolean lock(String key,long expireTime,TimeUnit timeUnit){
        return redisTemplate.opsForValue().setIfAbsent(getKey(key),1,expireTime, timeUnit);
    }

    public void unlock(String key){
        redisTemplate.delete(getKey(key));
    }
}
