package com.jjg.game.core.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.function.IntFunction;

/**
 * @author 11
 * @date 2025/6/18 16:12
 */
public abstract class AbstractPoolDao {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    //标准池
    protected final String pool_prefix = "pool:";
    //小奖池
    protected final String small_pool_prefix = "smallPool:";
    //小奖池(假)
    protected final String fake_small_pool_prefix = "fakeSmallPool:";

    //房间池
    protected final String room_pool_prefix = "roomPool";


    /**
     * Lua 脚本：批量 HGETALL，所有操作在 Redis 服务端一次性执行
     * KEYS: smallPool:gameType 列表
     * 返回: [[field,val,...], [field,val,...], ...]
     */
    private final RedisScript<List> BATCH_HGETALL_SCRIPT = RedisScript.of(
            """
                    local result = {}
                    for i = 1, #KEYS do
                        result[i] = redis.call('HGETALL', KEYS[i])
                    end
                    return result
                    """,
            List.class
    );

    /**
     * 根据场次id获取池子
     *
     * @param roomCfgId
     * @return
     */
    public Number getBigPoolByRoomCfgId(int gameType, int roomCfgId) {
        return (Number) redisTemplate.opsForHash().get(tableName(gameType), roomCfgId);
    }

    /**
     * 根据场次id获取池子
     *
     * @param roomCfgId
     * @return
     */
    public Number getSmallPoolByRoomCfgId(int gameType, int roomCfgId) {
        return (Number) redisTemplate.opsForHash().get(smallTableName(gameType), roomCfgId);
    }

    /**
     * 根据场次id获取池子
     *
     * @param roomCfgId
     * @return
     */
    public Number getFakeSmallPoolByRoomCfgId(int gameType, int roomCfgId) {
        return (Number) redisTemplate.opsForHash().get(fakeSmallTableName(gameType), roomCfgId);
    }

    /**
     * 池子加钱
     *
     * @param roomCfgId
     * @param value
     * @return
     */
    public Long add(int gameType, int roomCfgId, long value) {
        if (value < 1) {
            log.debug("池子添加金币时，value不能小于0  gameType = {},roomCfgId = {},value = {}", gameType, roomCfgId, value);
            return null;
        }
        return redisTemplate.opsForHash().increment(tableName(gameType), roomCfgId, value);
    }

    /**
     * 池子减钱
     *
     * @param roomCfgId
     * @param value
     * @return
     */
    public Long reduce(int gameType, int roomCfgId, long value) {
        if (value > -1) {
            log.debug("池子减少金币时，value不能大于-1  gameType = {},roomCfgId = {},value = {}", gameType, roomCfgId, value);
            return null;
        }

        long after = redisTemplate.opsForHash().increment(tableName(gameType), roomCfgId, value);
        if (after < 0) {
            //如果减去value后，after为负数，则要回滚
            //因为池子减为负数的情况基本不可能，所以采用回滚方式，这样可以避免加锁带来的延迟
            redisTemplate.opsForHash().increment(tableName(gameType), roomCfgId, Math.abs(value));
            log.debug("池子减少金币后小于0，所以进行回滚  gameType = {},roomCfgId = {},value = {}", gameType, roomCfgId, value);
            return null;
        }
        return after;
    }

    public Map<Object, Object> getSmallPoolByRoomCfgId(int gameType) {
        // 直接获取整个Hash（因为只有3个字段，HGETALL最有效率）
        return redisTemplate.opsForHash().entries(smallTableName(gameType));
    }

    public Map<Object, Object> getFakeSmallPoolByRoomCfgId(int gameType) {
        return redisTemplate.opsForHash().entries(fakeSmallTableName(gameType));
    }

    protected String tableName(int gameType) {
        return pool_prefix + gameType;
    }

    protected String smallTableName(int gameType) {
        return small_pool_prefix + gameType;
    }

    protected String fakeSmallTableName(int gameType) {
        return fake_small_pool_prefix + gameType;
    }

    public Map<Integer, Map<Integer, Long>> getSmallPools(List<Integer> gameTypeList) {
        return batchHGetAll(gameTypeList, this::smallTableName);
    }

    public Map<Integer, Map<Integer, Long>> getFakeSmallPools(List<Integer> gameTypeList) {
        return batchHGetAll(gameTypeList, this::fakeSmallTableName);
    }

    public Map<Integer, Map<Integer, Long>> getBigPools(List<Integer> gameTypeList) {
        return batchHGetAll(gameTypeList, this::tableName);
    }

    /**
     * 批量 HGETALL 公共实现：单次 Lua 调用，服务端执行所有 HGETALL
     *
     * @param gameTypeList 游戏列表
     * @param keyMapper    场次 -> Redis key 的映射函数
     */
    private Map<Integer, Map<Integer, Long>> batchHGetAll(List<Integer> gameTypeList,
                                                          IntFunction<String> keyMapper) {
        if (gameTypeList == null || gameTypeList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = new ArrayList<>(gameTypeList.size());
        for (Integer gameType : gameTypeList) {
            keys.add(keyMapper.apply(gameType));
        }

        List<List<String>> rawResults = (List<List<String>>) stringRedisTemplate.execute(BATCH_HGETALL_SCRIPT, keys);

        if (rawResults == null) {
            log.error("batchHGetAll Lua 执行返回 null，keys: {}", keys);
            return Collections.emptyMap();
        }

        Map<Integer, Map<Integer, Long>> map = new HashMap<>(gameTypeList.size());
        if (rawResults.size() == gameTypeList.size()) {
            for (int i = 0; i < gameTypeList.size(); i++) {
                List<String> flatList = rawResults.get(i);
                Map<Integer, Long> hashMap = new HashMap<>();
                if (flatList != null && !flatList.isEmpty()) {
                    for (int j = 0; j + 1 < flatList.size(); j += 2) {
                        int roomCfgId = Integer.parseInt(flatList.get(j));
                        long value = Long.parseLong(flatList.get(j + 1));
                        hashMap.put(roomCfgId, value);
                    }
                    map.put(gameTypeList.get(i), hashMap);
                }
            }
        } else {
            log.error("batchHGetAll 返回结果数量异常，期望: {}，实际: {}", gameTypeList.size(), rawResults.size());
        }
        return map;
    }
}
