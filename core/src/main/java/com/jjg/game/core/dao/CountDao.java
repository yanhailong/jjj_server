package com.jjg.game.core.dao;

import com.jjg.game.core.utils.RedisUtils;
import org.redisson.api.*;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
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
    private static final String DECREMENT_IF_SUFFICIENT = """
            local current_str = redis.call('GET', KEYS[1])
            if not current_str or current_str == '' then
                return nil
            end
            local current_val = tonumber(current_str)
            local amount = tonumber(ARGV[1])
            if current_val < amount then
                return nil
            end
            local new_val = redis.call('DECRBY', KEYS[1], amount)
            return new_val
            """;
    private static final String GET_COUNTS = """
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
    private static final String INCREMENT_WITHOUT_EXPIRE_REFRESH = """
            local exists = redis.call('EXISTS', KEYS[1])
            local current_value = redis.call('INCRBY', KEYS[1], ARGV[1])
            if exists == 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            return current_value
            """;
    private static final String LUA_SCRIPT = """
            redis.call('SET', KEYS[1], ARGV[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return 1
            """;
    private Logger log = LoggerFactory.getLogger(getClass());

    private final RedissonClient redissonClient;
    private final String TABLE_NAME = "count:%s:%s";
    private final String HASH_TABLE_NAME = "count:%s";

    public CountDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private String getKey(String featureId, String customId) {
        return String.format(TABLE_NAME, featureId, customId);
    }

    private String getHashKey(String featureId) {
        return String.format(HASH_TABLE_NAME, featureId);
    }

    /**
     * 设置计数（保留两位小数）
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @param count     计数
     */
    public void setCount(String featureId, String customId, BigDecimal count) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(getKey(featureId, customId));
        atomicLong.set(RedisUtils.toLong(count));
    }

    /**
     * 设置计数（保留两位小数）
     *
     * @param featureId     功能ID
     * @param customId      功能子ID
     * @param count         计数
     * @param expireSeconds 过期时间
     */
    public void setCount(String featureId, String customId, BigDecimal count, long expireSeconds) {
        String key = getKey(featureId, customId);
        long value = RedisUtils.toLong(count);
        redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE, LUA_SCRIPT, RScript.ReturnType.VALUE,
                        Collections.singletonList(key),
                        String.valueOf(value),
                        String.valueOf(expireSeconds));
    }

    /**
     * 原子地设置/自增值，仅在第一次设置时设置过期时间。
     * 如果键不存在，则设置为 count 的值并设置过期时间。
     * 如果键存在，则自增 count 的值，但不刷新过期时间。
     *
     * @param featureId     特性 ID
     * @param customId      业务 ID
     * @param count         要设置或自增的值
     * @param expireSeconds 第一次设置时使用的过期时间（秒）
     * @return 自增或设置后的新值
     */
    public BigDecimal incrementWithoutExpireRefresh(String featureId, String customId, BigDecimal count, long expireSeconds) {
        String key = getKey(featureId, customId);
        long incrementValue = RedisUtils.toLong(count);

        // Lua 脚本：实现原子操作
        // KEYS[1]: key
        // ARGV[1]: incrementValue (自增的值)
        // ARGV[2]: expireSeconds (过期时间)
        // 执行 Lua 脚本
        Object eval = redissonClient.getScript(StringCodec.INSTANCE)
                .eval(
                        RScript.Mode.READ_WRITE,
                        INCREMENT_WITHOUT_EXPIRE_REFRESH,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(key),
                        String.valueOf(incrementValue),
                        String.valueOf(expireSeconds)
                );
        if (eval instanceof Long addAfterValue) {
            return RedisUtils.fromLong(addAfterValue);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 原子地执行条件自减操作。
     * 只有当键存在且当前值 >= 要自减的值时，才执行自减。
     * 如果自减成功，返回新的值；否则返回 null。
     *
     * @param featureId      特性 ID
     * @param customId       业务 ID
     * @param decrementValue 要自减的值 (必须为正数)
     * @return 成功自减后的新值 (Long)，失败返回 null。
     */
    public BigDecimal decrementIfSufficient(String featureId, String customId, BigDecimal decrementValue) {
        String key = getKey(featureId, customId);
        // 确保 decrementValue 是正数，我们用负数进行比较
        long amountToDecrement = RedisUtils.toLong(decrementValue);

        // Lua 脚本：实现原子操作
        // KEYS[1]: key
        // ARGV[1]: amountToDecrement (要自减的正值)

        // 注意：我们将 Long.valueOf(null) 作为失败结果
        // 执行 Lua 脚本
        Object eval = redissonClient.getScript(StringCodec.INSTANCE)
                .eval(
                        RScript.Mode.READ_WRITE,
                        DECREMENT_IF_SUFFICIENT,
                        RScript.ReturnType.INTEGER,
                        Collections.singletonList(key),
                        String.valueOf(amountToDecrement)
                );
        if (eval instanceof Long result) {
            return RedisUtils.fromLong(result);
        }
        return null;
    }

    /**
     * 获取计数（返回 BigDecimal）
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @return 计数
     */
    public BigDecimal getCount(String featureId, String customId) {
        String key = getKey(featureId, customId);
        long val = redissonClient.getAtomicLong(key).get();
        return RedisUtils.fromLong(val);
    }

    public Long getCountLong(String featureId, String customId) {
        String key = getKey(featureId, customId);
        return redissonClient.getAtomicLong(key).get();
    }

    public BigDecimal getCountHash(String featureId, String customId) {
        String hashKey = getHashKey(featureId);
        RMap<String, Long> map = redissonClient.getMap(hashKey, LongCodec.INSTANCE);
        Long l = map.get(customId);
        if (l == null) {
            return BigDecimal.ZERO;
        }
        return RedisUtils.fromLong(l);
    }

    public Long getLongCount(String featureId, String customId) {
        String key = getKey(featureId, customId);
        return redissonClient.getAtomicLong(key).get();
    }

    /**
     * 原子自增（两位小数）
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @param delta     自增步数
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
     *
     * @param customId
     * @param deltaMap
     * @return
     */
    public Map<String, Long> incrBy(String customId, Map<String, Long> deltaMap) {
        if (deltaMap == null || deltaMap.isEmpty()) {
            return Collections.emptyMap();
        }
        RBatch batch = redissonClient.createBatch();

        Map<String, RFuture<Long>> tmpMap = new HashMap<>();
        for (Map.Entry<String, Long> en : deltaMap.entrySet()) {
            String key = getKey(en.getKey(), customId);
//            long deltaLong = RedisUtils.toLong(en.getValue());
            RFuture<Long> amountAsync = batch.getAtomicLong(key).addAndGetAsync(en.getValue());
            tmpMap.put(en.getKey(), amountAsync);
        }

        //批量执行
        batch.execute();

        try {
            Map<String, Long> resMap = new HashMap<>(tmpMap.size());
            for (Map.Entry<String, RFuture<Long>> en : tmpMap.entrySet()) {
                RFuture<Long> amountAsync = en.getValue();
                Long newAmount = amountAsync.get();
                resMap.put(en.getKey(), newAmount);
            }
            return resMap;
        } catch (Exception e) {
            log.debug("获取执行后结果异常");
            return Collections.emptyMap();
        }
    }

    /**
     * 自增1.00
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @return 自增后的值
     */
    public BigDecimal incr(String featureId, String customId) {
        return incrBy(featureId, customId, BigDecimal.ONE);
    }

    /**
     * 重置计数
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     */
    public void reset(String featureId, String customId) {
        redissonClient.getAtomicLong(getKey(featureId, customId)).delete();
    }

    /**
     * 是否存在
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @return true存在 false不存在
     */
    public boolean exists(String featureId, String customId) {
        return redissonClient.getAtomicLong(getKey(featureId, customId)).isExists();
    }

    /**
     * 如果不存在就设置值
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @return true 设置成功 false设置失败
     */
    public boolean setIfAbsent(String featureId, String customId) {
        return setIfAbsent(featureId, customId, BigDecimal.ONE);
    }

    /**
     * 如果不存在就设置值
     *
     * @param featureId 功能ID
     * @param customId  功能子ID
     * @return true 设置成功 false设置失败
     */
    public boolean setIfAbsent(String featureId, String customId, BigDecimal delta) {
        long deltaLong = RedisUtils.toLong(delta);
        return redissonClient.getAtomicLong(getKey(featureId, customId)).compareAndSet(0, deltaLong);
    }

    public boolean setIfAbsentHash(String featureId, String customId) {
        String hashKey = getHashKey(featureId);
        RMap<String, Long> map = redissonClient.getMap(hashKey, LongCodec.INSTANCE);
        return map.fastPutIfAbsent(customId, 100L);
    }

    /**
     * 获取多个
     *
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

        List<Long> values = redissonClient.getScript()
                .eval(RScript.Mode.READ_ONLY, GET_COUNTS, RScript.ReturnType.MULTI, objectKeys);
        Map<String, BigDecimal> result = new HashMap<>();
        for (int i = 0; i < customIds.size(); i++) {
            long v = values.get(i) == null ? 0L : values.get(i);
            result.put(customIds.get(i), RedisUtils.fromLong(v));
        }
        return result;
    }

    /**
     * 充值计数
     *
     * @param customId
     * @param value
     */
    public Map<String, Object> incrRechargeInfo(String customId, BigDecimal value) {
        Map<String, Long> map = new HashMap<>();

        map.put(CountType.RECHARGE.getParam(), RedisUtils.toLong(value));
        map.put(CountType.RECHARGE_COUNT.getParam(), 1L);
        Map<String, Long> tmpMap = incrBy(customId, map);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Long l = tmpMap.get(CountType.RECHARGE.getParam());

        Map<String, Object> resMap = new HashMap<>();
        resMap.put(CountType.RECHARGE.getParam(), RedisUtils.fromLong(l));
        resMap.put(CountType.RECHARGE_COUNT.getParam(), tmpMap.getOrDefault(CountType.RECHARGE_COUNT.getParam(), 0L));
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
        //玩家计数
        PLAYER_COUNT("player:%s"),
        //系统参数
        SYSTEM("system"),;
        private final String param;

        CountType(String param) {
            this.param = param;
        }

        public String getParam() {
            return param;
        }
    }
}
