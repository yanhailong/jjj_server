package com.jjg.game.account.dao;

import com.jjg.game.core.constant.GameConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用于生成或者获取playerId
 * @author 11
 * @date 2025/5/26 9:45
 */
@Repository
public class PlayerIdDao {
    private static final String tableName = "playerIncrId:";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 初始化id
     */
    public void init(){
        redisTemplate.opsForValue().setIfAbsent(tableName, GameConstant.Common.playerBeginId);
    }

    /**
     * 获取一个新的playerId
     */
    public long getNewId(){
        return redisTemplate.opsForValue().increment(tableName,1);
    }
}
