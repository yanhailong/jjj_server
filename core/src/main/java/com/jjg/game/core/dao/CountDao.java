package com.jjg.game.core.dao;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

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

    // ---------- 工具 ----------
    private long toLong(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }

    private BigDecimal fromLong(long value) {
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * 设置计数（保留两位小数）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @param count 计数
     */
    public void setCount(String featureId, String customId, BigDecimal count) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(getKey(featureId, customId));
        atomicLong.set(toLong(count));
    }

    /**
     * 设置计数（保留两位小数）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @param count 计数
     * @param expireSeconds 过期时间
     */
    public void setCount(String featureId, String customId, BigDecimal count, long expireSeconds) {
        String key = getKey(featureId, customId);
        long value = toLong(count);
        String lua = """
                redis.call('SET', KEYS[1], ARGV[1])
                redis.call('EXPIRE', KEYS[1], ARGV[2])
                return 1
                """;
        redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE, lua, RScript.ReturnType.VALUE,
                        Collections.singletonList(key),
                        String.valueOf(value),
                        String.valueOf(expireSeconds));
    }

    /**
     * 获取计数（返回 BigDecimal）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @return 计数
     */
    public BigDecimal getCount(String featureId, String customId) {
        String key = getKey(featureId, customId);
        long val = redissonClient.getAtomicLong(key).get();
        return fromLong(val);
    }

    /**
     * 原子自增（两位小数）
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @param delta 自增步数
     * @return 自增后的值
     */
    public BigDecimal incrBy(String featureId, String customId, BigDecimal delta) {
        String key = getKey(featureId, customId);
        long deltaLong = toLong(delta);
        long newVal = redissonClient.getAtomicLong(key).addAndGet(deltaLong);
        return fromLong(newVal);
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
        redissonClient.getAtomicLong(getKey(featureId, customId)).delete();
    }

    /**
     * 是否存在
     * @param featureId  功能ID
     * @param customId 功能子ID
     * @return true存在 false不存在
     */
    public boolean exists(String featureId, String customId) {
        return redissonClient.getAtomicLong(getKey(featureId, customId)).isExists();
    }

    /**
     * 如果不存在就设置值
     * @param featureId 功能ID
     * @param customId 功能子ID
     * @return true 设置成功 false设置失败
     */
    public boolean setIfAbsent(String featureId, String customId) {
        return redissonClient.getAtomicLong(getKey(featureId, customId)).compareAndSet(0, 1);
    }


}
