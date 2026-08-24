package com.jjg.game.core.dao;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class TogetherPlayReconnectDao {
    private static final String KEY_PREFIX = "togetherPlay:reconnect:";

    private final StringRedisTemplate redisTemplate;

    public TogetherPlayReconnectDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void mark(long playerId, int gameType, long expireMillis) {
        redisTemplate.opsForValue().set(
                key(playerId), String.valueOf(gameType), expireMillis, TimeUnit.MILLISECONDS);
    }

    public int getGameType(long playerId) {
        String value = redisTemplate.opsForValue().get(key(playerId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public void remove(long playerId) {
        redisTemplate.delete(key(playerId));
    }

    private String key(long playerId) {
        return KEY_PREFIX + playerId;
    }
}
