package com.jjg.game.core.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import com.jjg.game.common.redis.PlayerKeyIndex;

/**
 * @author 11
 * @date 2025/5/26 11:42
 */
@Repository
public class PlayerLoginTimeDao {
    private static final String TABLE_NAME = "playerLoginTime";

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private PlayerKeyIndex playerKeyIndex;

    public void add(long playerId, long timeMill) {
        redisTemplate.opsForZSet().add(TABLE_NAME, playerId, timeMill);
        playerKeyIndex.addZSetMember(playerId, TABLE_NAME, String.valueOf(playerId));
    }

    public Set<Object> getLoginSet(long expireTime) {
        return redisTemplate.opsForZSet().rangeByScore(TABLE_NAME, 0, expireTime);
    }

    public Double score(long playerId){
        return redisTemplate.opsForZSet().score(TABLE_NAME, playerId);
    }

    public void remove(long playerId) {
        redisTemplate.opsForZSet().remove(TABLE_NAME, playerId);
        playerKeyIndex.removeZSetMember(playerId, TABLE_NAME, String.valueOf(playerId));
    }
}
