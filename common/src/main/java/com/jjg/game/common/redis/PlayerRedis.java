package com.jjg.game.common.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author lm
 * @date 2026/1/22 14:18
 */
@Component
public class PlayerRedis {
    private final RedisTemplate<String, String> redisTemplate;
    private final PlayerKeyIndex index;

    public PlayerRedis(RedisTemplate<String, String> redisTemplate, PlayerKeyIndex index) {
        this.redisTemplate = redisTemplate;
        this.index = index;
    }

    public void delete(long playerId, String key) {
        redisTemplate.delete(key);
        index.remove(playerId, key);
    }

    // ===== String =====
    public void set(long playerId, String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        index.add(playerId, key);
    }

    public Long incr(long playerId, String key, long delta) {
        Long v = redisTemplate.opsForValue().increment(key, delta);
        index.add(playerId, key);
        return v;
    }

    // ===== Hash =====
    public void hset(long playerId, String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
        index.addHash(playerId, key, field);
    }

    public void hsetAll(long playerId, String key, Map<String, String> data) {
        redisTemplate.opsForHash().putAll(key, data);
        index.addHashBatch(playerId, key, data.keySet());
    }

    public Long hincr(long playerId, String key, String field, long delta) {
        Long v = redisTemplate.opsForHash().increment(key, field, delta);
        index.addHash(playerId, key, field);
        return v;
    }

    public void hdelete(long playerId, String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
        index.removeHash(playerId, key, field);
    }

}
