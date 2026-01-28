package com.jjg.game.common.redis;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author lm
 * @date 2026/1/22 14:22
 */
@Component
public class PlayerKeyIndex {
    public static final String PLAYER_KEY_PREFIX = "player_keys:";
    private final RedisTemplate<String, String> redis;

    public PlayerKeyIndex(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void add(long playerId, String key) {
        redis.opsForSet().add(PLAYER_KEY_PREFIX + playerId, key);
    }

    public void addHash(long playerId, String key, String field) {
        redis.opsForSet().add(PLAYER_KEY_PREFIX + playerId, key + "#" + field);
    }

    // 批量（给在线写入 / 迁移用）
    public void addHashBatch(long playerId, String key, Collection<String> fields) {
        List<String> values = fields.stream().map(k -> key + "#" + k).toList();
        addBatch(playerId, values);
    }

    public void addBatch(long playerId, Collection<String> values) {
        redis.executePipelined((RedisCallback<Object>) conn -> {
            byte[] index = (PLAYER_KEY_PREFIX + playerId).getBytes();
            for (String k : values) {
                conn.setCommands().sAdd(index, k.getBytes());
            }
            return null;
        });
    }

    public void removeHash(long playerId, String key, String field) {
        redis.opsForSet().remove(PLAYER_KEY_PREFIX + playerId, key + "#" + field);
    }

    public Set<String> getKeys(long playerId) {
        return redis.opsForSet().members(PLAYER_KEY_PREFIX + playerId);
    }

    public void remove(long playerId, String key) {
        redis.opsForSet().remove(PLAYER_KEY_PREFIX + playerId, key);
    }

    public void clear(long playerId) {
        redis.delete(PLAYER_KEY_PREFIX + playerId);
    }
}
