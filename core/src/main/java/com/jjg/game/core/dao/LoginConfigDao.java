package com.jjg.game.core.dao;

import com.jjg.game.core.data.LoginConfigData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/10/22 11:56
 */
@Repository
public class LoginConfigDao {
    private final String TABLE_NAME = "gm:loginConfig:";

    @Autowired
    private RedisTemplate redisTemplate;

    private String tableName(int channel) {
        return TABLE_NAME + channel;
    }

    public void save(int channel, Map<Integer, LoginConfigData> map) {
        String key = tableName(channel);
        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(tableName(channel), map);
    }


    public Map getAll(int channel) {
        return redisTemplate.opsForHash().entries(tableName(channel));
    }

}
