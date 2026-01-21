package com.jjg.game.core.dao;

import com.jjg.game.core.data.SmsConfigInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2026/1/19
 */
@Repository
public class SmsConfigDao {
    private final String TABLE_NAME = "gm:smsConfig";

    @Autowired
    private RedisTemplate redisTemplate;

    public List<SmsConfigInfo> getAll() {
        return redisTemplate.opsForHash().values(TABLE_NAME);
    }

    public void save(Map<Integer, SmsConfigInfo> map) {
        redisTemplate.delete(TABLE_NAME);
        if(map != null || !map.isEmpty()){
            redisTemplate.opsForHash().putAll(TABLE_NAME, map);
        }
    }
}
