package com.jjg.game.core.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 通用dao
 */
@Repository
public class CommonDao {
    @Autowired
    private RedisTemplate redisTemplate;

    private final String COMMON_TABLE_NAME = "common";

    public void setValue(int id, String value) {
        redisTemplate.opsForHash().put(COMMON_TABLE_NAME, id, value);
    }

    public String getStrValue(int id) {
        return (String) redisTemplate.opsForHash().get(COMMON_TABLE_NAME, id);
    }
}
