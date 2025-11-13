package com.jjg.game.core.dao;

import com.jjg.game.core.utils.RedisUtils;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

/**
 * 通用计数DAO
 * 支持两位小数，并在Redis层实现原子自增。
 * key 格式：count:{featureId}:{customId}
 */
@Repository
public class CountDao {

    private Logger log = LoggerFactory.getLogger(getClass());

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
        RAtomicLong atomicLong = redissonClient.getAtomicLong(getKey(featureId, customId));
        atomicLong.set(RedisUtils.toLong(count));
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
        long value = RedisUtils.toLong(count);
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
        return RedisUtils.fromLong(val);
    }

    public Long getLongCount(String featureId, String customId) {
        String key = getKey(featureId, customId);
        return redissonClient.getAtomicLong(key).get();
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
        long deltaLong = RedisUtils.toLong(delta);
        long newVal = redissonClient.getAtomicLong(key).addAndGet(deltaLong);
        return RedisUtils.fromLong(newVal);
    }

    /**
     * 原子批量自增（两位小数）
     * @param customId
     * @param deltaMap
     * @return
     */
    public Map<String,Long> incrBy(String customId, Map<String,Long>  deltaMap) {
        if(deltaMap == null || deltaMap.isEmpty()){
            return Collections.emptyMap();
        }
        RBatch batch = redissonClient.createBatch();

        Map<String,RFuture<Long>> tmpMap = new HashMap<>();
        for(Map.Entry<String,Long> en : deltaMap.entrySet()){
            String key = getKey(en.getKey(), customId);
//            long deltaLong = RedisUtils.toLong(en.getValue());
            RFuture<Long> amountAsync = batch.getAtomicLong(key).addAndGetAsync(en.getValue());
            tmpMap.put(en.getKey(),amountAsync);
        }

        //批量执行
        batch.execute();

        try{
            Map<String,Long> resMap = new HashMap<>(tmpMap.size());
            for(Map.Entry<String,RFuture<Long>> en : tmpMap.entrySet()){
                RFuture<Long> amountAsync = en.getValue();
                Long newAmount = amountAsync.get();
                resMap.put(en.getKey(),newAmount);
            }
            return resMap;
        }catch (Exception e){
            log.debug("获取执行后结果异常");
            return Collections.emptyMap();
        }
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

    /**
     * 获取多个
     * @param featureId 功能id
     * @param customIds 功能子id集合
     * @return
     */
    public Map<String, BigDecimal> getCounts(String featureId, List<String> customIds) {
        if (customIds == null || customIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 拼接 Redis key
        List<String> keys = customIds.stream()
                .map(customId -> getKey(featureId, customId))
                .toList();
        List<Object> objectKeys = new ArrayList<>(keys);
        // Lua 一次性批量获取
        // Lua: 获取并尝试转换为 number，缺失则返回 0
        String lua = """
            local res = {}
            for i, k in ipairs(KEYS) do
                local v = redis.call('GET', k)
                if not v then
                    table.insert(res, 0)
                else
                    table.insert(res, tonumber(v))
                end
            end
            return res
            """;

        List<Long> values = redissonClient.getScript()
                .eval(RScript.Mode.READ_ONLY, lua, RScript.ReturnType.MULTI, objectKeys);

        Map<String, BigDecimal> result = new HashMap<>();
        for (int i = 0; i < customIds.size(); i++) {
            long v = values.get(i) == null ? 0L : values.get(i);
            result.put(customIds.get(i), RedisUtils.fromLong(v));
        }
        return result;
    }

    /**
     * 充值计数
     * @param customId
     * @param value
     */
    public Map<String,Object> incrRechargeInfo(String customId,BigDecimal value) {
        Map<String,Long> map = new HashMap<>();

        map.put(CountType.RECHARGE.getParam(),RedisUtils.toLong(value));
        map.put(CountType.RECHARGE_COUNT.getParam(),1L);
        Map<String, Long> tmpMap = incrBy(customId, map);
        if(tmpMap == null || tmpMap.isEmpty()){
            return Collections.emptyMap();
        }

        Long l = tmpMap.get(CountType.RECHARGE.getParam());

        Map<String,Object> resMap = new HashMap<>();
        resMap.put(CountType.RECHARGE.getParam(),RedisUtils.fromLong(l));
        resMap.put(CountType.RECHARGE_COUNT.getParam(),tmpMap.getOrDefault(CountType.RECHARGE_COUNT.getParam(),0L));
        return resMap;
    }

    /**
     * 条件功能枚举
     */
    public enum CountType {
        //活动掉落计数
        ACTIVITY_CONDITIONS("activity:%s"),
        //我的赌场掉落计数
        MY_CASINO("myCasino"),
        //活动状态
        ACTIVITY_STATUS("activity:status:%s"),
        //活动计数
        ACTIVITY_COUNT("activity:count:%s"),
        //充值金额
        RECHARGE("recharge"),
        //充值次数
        RECHARGE_COUNT("recharge:count"),
        ;
        private final String param;

        CountType(String param) {
            this.param = param;
        }

        public String getParam() {
            return param;
        }
    }
}
