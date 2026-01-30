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
    private static final String LIST_PREFIX = "L:";
    private static final String SET_PREFIX = "S:";
    private static final String ZSET_PREFIX = "Z:";
    private final RedisTemplate<String, String> redis;

    public PlayerKeyIndex(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void add(long playerId, String key) {
        redis.opsForSet().add(PLAYER_KEY_PREFIX + playerId, key);
    }

    public void addListKey(long playerId, String key) {
        add(playerId, LIST_PREFIX + key);
    }

    public void addSetKey(long playerId, String key) {
        add(playerId, SET_PREFIX + key);
    }

    public void addZSetKey(long playerId, String key) {
        add(playerId, ZSET_PREFIX + key);
    }

    public void addSetMember(long playerId, String key, String member) {
        add(playerId, SET_PREFIX + key + "#" + member);
    }

    public void addZSetMember(long playerId, String key, String member) {
        add(playerId, ZSET_PREFIX + key + "#" + member);
    }

    public void addHash(long playerId, String key, String field) {
        redis.opsForSet().add(PLAYER_KEY_PREFIX + playerId, key + "#" + field);
    }

    // 批量（给在线写入 / 迁移用）
    public void addHashBatch(long playerId, String key, Collection<String> fields) {
        List<String> values = fields.stream().map(k -> key + "#" + k).toList();
        addBatch(playerId, values);
    }

    public void removeHashBatch(long playerId, String key, Collection<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        List<String> values = fields.stream().map(k -> key + "#" + k).toList();
        removeBatch(playerId, values);
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

    private void removeBatch(long playerId, Collection<String> values) {
        redis.executePipelined((RedisCallback<Object>) conn -> {
            byte[] index = (PLAYER_KEY_PREFIX + playerId).getBytes();
            for (String k : values) {
                conn.setCommands().sRem(index, k.getBytes());
            }
            return null;
        });
    }

    public void removeHash(long playerId, String key, String field) {
        redis.opsForSet().remove(PLAYER_KEY_PREFIX + playerId, key + "#" + field);
    }

    public void removeListKey(long playerId, String key) {
        remove(playerId, LIST_PREFIX + key);
    }

    public void removeSetKey(long playerId, String key) {
        remove(playerId, SET_PREFIX + key);
    }

    public void removeZSetKey(long playerId, String key) {
        remove(playerId, ZSET_PREFIX + key);
    }

    public void removeSetMember(long playerId, String key, String member) {
        remove(playerId, SET_PREFIX + key + "#" + member);
    }

    public void removeZSetMember(long playerId, String key, String member) {
        remove(playerId, ZSET_PREFIX + key + "#" + member);
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
