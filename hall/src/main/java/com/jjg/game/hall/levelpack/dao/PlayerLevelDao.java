package com.jjg.game.hall.levelpack.dao;

import com.jjg.game.hall.levelpack.data.PlayerLevelPackData;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * 玩家等级礼包 DAO
 * 负责玩家等级礼包数据在 Redis 中的读写操作
 */
@Repository
public class PlayerLevelDao {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TABLE_NAME = "playerlevelpack:%d";       // 玩家等级礼包表
    private static final String TABLE_LOCK = "playerlevelpack:lock:%d";  // 玩家等级礼包锁

    public PlayerLevelDao(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拼接 Redis Key
     */
    private String getKey(long playerId) {
        return String.format(TABLE_NAME, playerId);
    }

    /**
     * 拼接锁 Key
     */
    public String getLockKey(long playerId) {
        return String.format(TABLE_LOCK, playerId);
    }

    /**
     * 获取玩家等级礼包数据
     */
    public Map<Integer, PlayerLevelPackData> getPlayerLevelPackData(long playerId) {
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        return hash.entries(getKey(playerId));
    }

    /**
     * 获取玩家等级礼包数据
     */
    public PlayerLevelPackData getPlayerLevelPackData(long playerId, int id) {
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        return hash.get(getKey(playerId), id);
    }


    /**
     * 保存单个礼包数据
     */
    public void savePackData(long playerId, int packId, PlayerLevelPackData data) {
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        hash.put(getKey(playerId), packId, data);
    }

    /**
     * 批量保存礼包数据
     */
    public void saveAllPackData(long playerId, Map<Integer, PlayerLevelPackData> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) {
            return;
        }
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        hash.putAll(getKey(playerId), dataMap);
    }

    /**
     * 删除某个礼包数据
     */
    public void removePackData(long playerId, int packId) {
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        hash.delete(getKey(playerId), packId);
    }

    /**
     * 判断是否存在某个礼包
     */
    public boolean exists(long playerId, int packId) {
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        return hash.hasKey(getKey(playerId), packId);
    }

    private HashOperations<String, Integer, PlayerLevelPackData> getOpsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * 删除整个玩家礼包表
     */
    public void clearPlayerData(long playerId) {
        redisTemplate.delete(getKey(playerId));
    }

}
