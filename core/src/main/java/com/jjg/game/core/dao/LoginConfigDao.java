package com.jjg.game.core.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * @author 11
 * @date 2025/10/22 11:56
 */
@Repository
public class LoginConfigDao {
    private final String TABLE_NAME = "gm:loginConfig";

    @Autowired
    private RedisTemplate redisTemplate;

    public void save(int loginType,boolean open){
        redisTemplate.opsForHash().put(TABLE_NAME, loginType, open);
    }

    public Map getAll(){
        return redisTemplate.opsForHash().entries(TABLE_NAME);
    }
}
