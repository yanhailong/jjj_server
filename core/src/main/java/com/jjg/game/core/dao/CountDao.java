package com.jjg.game.core.dao;

import org.redisson.api.RAtomicDouble;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * 通用计数DAO
 * 支持两位小数，并在Redis层实现原子自增。
 * key 格式：count:{featureId}:{customId}
 */
@Repository
public class CountDao {

    private final RedissonClient redissonClient;
    private static final String TABLE_NAME = "count:%s:%s";

    public CountDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private String getKey(String featureId, String customId) {
        return String.format(TABLE_NAME, featureId, customId);
    }

    /**
     * 设置计数（保留两位小数）
     */
    public void setCount(String featureId, String customId, BigDecimal count) {
        RBucket<Double> bucket = redissonClient.getBucket(getKey(featureId, customId));
        bucket.set(count.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    public void setCount(String featureId, String customId, BigDecimal count, long expireSeconds) {
        RBucket<Double> bucket = redissonClient.getBucket(getKey(featureId, customId));
        bucket.set(count.setScale(2, RoundingMode.HALF_UP).doubleValue(), Duration.ofSeconds(expireSeconds));
    }

    /**
     * 获取计数（返回 BigDecimal）
     */
    public BigDecimal getCount(String featureId, String customId) {
        RBucket<Double> bucket = redissonClient.getBucket(getKey(featureId, customId));
        Double val = bucket.get();
        return val == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 原子自增（两位小数）
     */
    public BigDecimal incrBy(String featureId, String customId, BigDecimal delta) {
        RAtomicDouble atomic = redissonClient.getAtomicDouble(getKey(featureId, customId));
        double newVal = atomic.addAndGet(delta.doubleValue());
        return BigDecimal.valueOf(newVal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 自增1.00
     */
    public BigDecimal incr(String featureId, String customId) {
        return incrBy(featureId, customId, BigDecimal.ONE);
    }

    /**
     * 重置计数
     */
    public void reset(String featureId, String customId) {
        redissonClient.getBucket(getKey(featureId, customId)).delete();
    }

    /**
     * 是否存在
     */
    public boolean exists(String featureId, String customId) {
        return redissonClient.getBucket(getKey(featureId, customId)).isExists();
    }
}
