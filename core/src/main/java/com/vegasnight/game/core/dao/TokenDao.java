package com.vegasnight.game.core.dao;

import com.vegasnight.game.common.utils.TimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author 11
 * @date 2025/5/26 10:14
 */
@Repository
public class TokenDao {
    private final String tableName = "loginToken:";
    private final String expireTableName = "loginTokenExpire:";

    //token过期时长
    private final long tokenExpireTime = 2 * TimeHelper.ONE_HOUR;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 未使用过的token进行保存
     * @param token
     * @param playerId
     */
    public void save(String token,long playerId){
        redisTemplate.opsForHash().put(tableName,token,playerId);
        //设置过期时间
        redisTemplate.opsForZSet().add(expireTableName, token, System.currentTimeMillis() + tokenExpireTime);
    }

    /**
     * 登录时的token是否有效
     * @param token
     * @return
     */
    public Long getPlayerIdByToken(String token){
        Object o = redisTemplate.opsForHash().get(tableName, token);
        if(o == null){
            return null;
        }

        return Long.parseLong(o.toString());
    }

    /**
     * 清除过期token
     */
    public void clearExpireToken(){
        long now = System.currentTimeMillis();
        long expire = now - tokenExpireTime;
        Set<Object> set = redisTemplate.opsForZSet().rangeByScore(expireTableName, 0, expire);
        if(set == null || set.isEmpty()){
            return;
        }

        Long tokenCount = redisTemplate.opsForHash().delete(tableName,set.toArray(new String[0]));
        Long expireCount = redisTemplate.opsForZSet().removeRangeByScore(expireTableName, 0, expire);
    }

    public void removeToken(String token){
        redisTemplate.opsForHash().delete(tableName,token);
        redisTemplate.opsForZSet().remove(expireTableName,token);
    }
}
