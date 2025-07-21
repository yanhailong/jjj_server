package com.jjg.game.core.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author 11
 * @date 2025/6/18 16:12
 */
public abstract class AbstractPoolDao {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected RedisTemplate redisTemplate;

    //标准池
    protected final String pool_prefix = "pool:";
    //小奖池
    protected final String small_pool_prefix = "smallPool:";

    public abstract void initPool();

    /**
     * 根据场次id获取池子
     * @param wareId
     * @return
     */
    public Number getByWareId(int gameType,int wareId){
        return (Number)redisTemplate.opsForHash().get(tableName(gameType),wareId);
    }

    /**
     * 池子加钱
     * @param wareId
     * @param value
     * @return
     */
    public Long add(int gameType,int wareId,long value){
        if(value < 1){
            log.debug("池子添加金币时，value不能小于0  gameType = {},wareId = {},value = {}", gameType,wareId, value);
            return null;
        }
        return redisTemplate.opsForHash().increment(tableName(gameType),wareId,value);
    }

    /**
     * 池子减钱
     * @param wareId
     * @param value
     * @return
     */
    public Long reduce(int gameType,int wareId,long value){
        if(value > -1){
            log.debug("池子减少金币时，value不能大于-1  gameType = {},wareId = {},value = {}", gameType,wareId, value);
            return null;
        }

        long after = redisTemplate.opsForHash().increment(tableName(gameType),wareId,value);
        if(after < 0){
            //如果减去value后，after为负数，则要回滚
            //因为池子减为负数的情况基本不可能，所以采用回滚方式，这样可以避免加锁带来的延迟
            redisTemplate.opsForHash().increment(tableName(gameType),wareId,Math.abs(value));
            log.debug("池子减少金币后小于0，所以进行回滚  gameType = {},wareId = {},value = {}", gameType,wareId, value);
            return null;
        }
        return after;
    }

    protected String tableName(int gameType){
        return pool_prefix + gameType;
    }
    protected String smallTableName(int gameType){
        return small_pool_prefix + gameType;
    }
}
