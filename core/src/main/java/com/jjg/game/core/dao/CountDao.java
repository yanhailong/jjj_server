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
    private final String TABLE_NAME = "count:%s:%s";

    public CountDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private String getKey(String featureId, String customId) {
        return String.format(TABLE_NAME, featureId, customId);
    }

    /**
     * 设置计数（保留两位小数）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @param count 计数
     */
    public void setCount(String featureId, String customId, BigDecimal count) {
        RAtomicDouble atomicDouble = redissonClient.getAtomicDouble(getKey(featureId, customId));
        atomicDouble.set(count.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    /**
     * 设置计数（保留两位小数）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @param count 计数
     * @param expireSeconds 过期时间
     */
    public void setCount(String featureId, String customId, BigDecimal count, long expireSeconds) {
        RBucket<Double> bucket = redissonClient.getBucket(getKey(featureId, customId));
        bucket.set(count.setScale(2, RoundingMode.HALF_UP).doubleValue(), Duration.ofSeconds(expireSeconds));
    }

    /**
     * 获取计数（返回 BigDecimal）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @return 计数
     */
    public BigDecimal getCount(String featureId, String customId) {
        RAtomicDouble atomicDouble = redissonClient.getAtomicDouble(getKey(featureId, customId));
        double val = atomicDouble.get();
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 原子自增（两位小数）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @param delta 自增步数
     * @return 自增后的值
     */
    public BigDecimal incrBy(String featureId, String customId, BigDecimal delta) {
        RAtomicDouble atomic = redissonClient.getAtomicDouble(getKey(featureId, customId));
        double newVal = atomic.addAndGet(delta.doubleValue());
        return BigDecimal.valueOf(newVal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 自增1.00
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @return 自增后的值
     */
    public BigDecimal incr(String featureId, String customId) {
        return incrBy(featureId, customId, BigDecimal.ONE);
    }

    /**
     * 重置计数
     * @param featureId 功能ID
     * @param customId 功能子ID
     */
    public void reset(String featureId, String customId) {
        redissonClient.getBucket(getKey(featureId, customId)).delete();
    }

    /**
     * 是否存在
     * @param featureId  功能ID
     * @param customId 功能子ID
     * @return true存在 false不存在
     */
    public boolean exists(String featureId, String customId) {
        return redissonClient.getBucket(getKey(featureId, customId)).isExists();
    }

    /**
     * 如果不存在就设置值
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @return true 设置成功 false设置失败
     */
    public boolean setIfAbsent(String featureId, String customId) {
        return redissonClient.getBucket(getKey(featureId, customId)).setIfAbsent(1);
    }


}
