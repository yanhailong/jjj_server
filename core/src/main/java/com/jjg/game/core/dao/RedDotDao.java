package com.jjg.game.core.dao;

import cn.hutool.core.util.EnumUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.redis.PlayerKeyIndex;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据结构：
 * 玩家id -> Hash(module<<16 | subModule -> value)
 * key: red_dot:{playerId}
 * field: long (module<<16 | subModule)
 * value: int (红点数量或状态)
 * 数据结构玩家id，模块,子模块(不超过100)，值（不超过100000）；子模块,模块合并成一个long存入redis
 *
 * @author lm
 * @date 2025/11/11 15:11
 */
@Repository
public class RedDotDao {

    private final RedissonClient redissonClient;
    private final PlayerKeyIndex playerKeyIndex;

    public RedDotDao(RedissonClient redissonClient, PlayerKeyIndex playerKeyIndex) {
        this.redissonClient = redissonClient;
        this.playerKeyIndex = playerKeyIndex;
    }

    private String getPlayerKey(long playerId) {
        return "red_dot:" + playerId;
    }

    /** 模块+子模块 合并成long */
    private long combineKey(int module, int subModule) {
        return ((long) module << 16) | (subModule & 0xFFFFL);
    }

    /** 设置红点值 */
    public void setValue(long playerId, int module, int subModule, int value) {
        RMap<Long, Integer> map = redissonClient.getMap(getPlayerKey(playerId));
        long key = combineKey(module, subModule);
        map.put(key, value);
        playerKeyIndex.addHash(playerId, getPlayerKey(playerId), String.valueOf(key));
    }

    /** 获取红点值 */
    public int getValue(long playerId, int module, int subModule) {
        RMap<Long, Integer> map = redissonClient.getMap(getPlayerKey(playerId));
        Integer val = map.get(combineKey(module, subModule));
        return val == null ? 0 : val;
    }


    /** 增加红点值 */
    public int incrementValue(long playerId, int module, int subModule, int delta) {
        RMap<Long, Integer> map = redissonClient.getMap(getPlayerKey(playerId));
        long key = combineKey(module, subModule);
        Integer value = map.getOrDefault(key, 0);
        if (value + delta <= 0) {
            map.put(key, 0);
            playerKeyIndex.addHash(playerId, getPlayerKey(playerId), String.valueOf(key));
            return 0;
        }
        int result = map.addAndGet(key, delta);
        playerKeyIndex.addHash(playerId, getPlayerKey(playerId), String.valueOf(key));
        return result;
    }

    /** 删除一个子模块红点 */
    public void delete(long playerId, int module, int subModule) {
        RMap<Long, Integer> map = redissonClient.getMap(getPlayerKey(playerId));
        long key = combineKey(module, subModule);
        map.remove(key);
        playerKeyIndex.removeHash(playerId, getPlayerKey(playerId), String.valueOf(key));
    }

    /** 获取玩家所有红点 */
    public Map<RedDotDetails.RedDotModule, Map<Integer, Integer>> getAll(long playerId) {
        RMap<Long, Integer> map = redissonClient.getMap(getPlayerKey(playerId));
        Map<RedDotDetails.RedDotModule, Map<Integer, Integer>> redDotInfo = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
            Pair<Integer, Integer> pair = decodeModule(entry.getKey());
            RedDotDetails.RedDotModule module = EnumUtil.getBy(RedDotDetails.RedDotModule.class, redDotModule -> redDotModule.getType() == pair.getFirst());
            if (module == null) {
                continue;
            }
            redDotInfo.computeIfAbsent(module, key -> new HashMap<>())
                    .put(pair.getSecond(), entry.getValue());
        }
        return redDotInfo;
    }

    /** 清空该玩家所有红点 */
    public void clear(long playerId) {
        RMap<Long, Integer> map = redissonClient.getMap(getPlayerKey(playerId));
        if (!map.isEmpty()) {
            playerKeyIndex.removeHashBatch(playerId, getPlayerKey(playerId),
                    map.keySet().stream().map(String::valueOf).toList());
        }
        map.clear();
    }

    /** 可选：调试用，反向解码模块名 */
    private Pair<Integer, Integer> decodeModule(long key) {
        int module = (int) (key >> 16);
        int subModule = (int) (key & 0xFFFF);
        return Pair.newPair(module, subModule);
    }
}
