package com.jjg.game.core.dao;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 通用计数DAO
 * 用于保存功能ID对应的计数或自定义ID的统计值。
 * key 格式：count:{featureId}:{customId}
 */
@Repository
public class CountDao {

    private final RedissonClient redissonClient;
    private static final String TABLE_NAME = "count:%s:%s";

    public CountDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 拼接Redis key
     */
    private String getKey(String featureId, String customId) {
        return String.format(TABLE_NAME, featureId, customId);
    }

    /**
     * 设置计数值
     */
    public void setCount(String featureId, String customId, long count) {
        RBucket<Long> bucket = redissonClient.getBucket(getKey(featureId, customId));
        bucket.set(count);
    }

    /**
     * 设置计数值并指定过期时间
     */
    public void setCount(String featureId, String customId, long count, long expireSeconds) {
        RBucket<Long> bucket = redissonClient.getBucket(getKey(featureId, customId));
        bucket.set(count, Duration.ofSeconds(expireSeconds));
    }

    /**
     * 获取计数值
     */
    public long getCount(String featureId, String customId) {
        RBucket<Long> bucket = redissonClient.getBucket(getKey(featureId, customId));
        Long val = bucket.get();
        return val == null ? 0L : val;
    }

    /**
     * 自增计数（返回自增后的值）
     */
    public long incr(String featureId, String customId) {
        return redissonClient.getAtomicLong(getKey(featureId, customId)).incrementAndGet();
    }

    /**
     * 自增指定步长
     */
    public long incrBy(String featureId, String customId, long delta) {
        return redissonClient.getAtomicLong(getKey(featureId, customId)).addAndGet(delta);
    }

    /**
     * 重置计数
     */
    public void reset(String featureId, String customId) {
        redissonClient.getBucket(getKey(featureId, customId)).delete();
    }

    /**
     * 判断是否存在
     */
    public boolean exists(String featureId, String customId) {
        return redissonClient.getBucket(getKey(featureId, customId)).isExists();
    }
}
