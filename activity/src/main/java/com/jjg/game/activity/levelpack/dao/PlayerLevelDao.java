package com.jjg.game.activity.levelpack.dao;

import com.jjg.game.activity.levelpack.data.PlayerLevelPackData;
import com.jjg.game.common.redis.PlayerKeyIndex;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;

/**
 * 玩家等级礼包 DAO
 * 负责玩家等级礼包数据在 Redis 中的读写操作
 */
@Repository
public class PlayerLevelDao {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlayerKeyIndex playerKeyIndex;
    private static final String TABLE_NAME = "playerlevelpack:%d";       // 玩家等级礼包表
    private static final String TABLE_LOCK = "playerlevelpack:lock:%d";  // 玩家等级礼包锁

    public PlayerLevelDao(RedisTemplate<String, Object> redisTemplate, PlayerKeyIndex playerKeyIndex) {
        this.redisTemplate = redisTemplate;
        this.playerKeyIndex = playerKeyIndex;
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
        playerKeyIndex.addHash(playerId, getKey(playerId), String.valueOf(packId));
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
        playerKeyIndex.addHashBatch(playerId, getKey(playerId), toStringKeys(dataMap.keySet()));
    }

    /**
     * 删除某个礼包数据
     */
    public void removePackData(long playerId, int packId) {
        HashOperations<String, Integer, PlayerLevelPackData> hash = getOpsForHash();
        hash.delete(getKey(playerId), packId);
        playerKeyIndex.removeHash(playerId, getKey(playerId), String.valueOf(packId));
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
        String key = getKey(playerId);
        java.util.Set<Integer> fields = getOpsForHash().keys(key);
        if (!fields.isEmpty()) {
            playerKeyIndex.removeHashBatch(playerId, key, toStringKeys(fields));
        }
        redisTemplate.delete(key);
    }

    private Collection<String> toStringKeys(Collection<Integer> keys) {
        return keys.stream().map(String::valueOf).toList();
    }
}
