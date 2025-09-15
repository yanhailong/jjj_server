package com.jjg.game.core.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

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
    //小奖池(假)
    protected final String fake_small_pool_prefix = "fakeSmallPool:";

    public abstract void initPool();

    /**
     * 根据场次id获取池子
     * @param roomCfgId
     * @return
     */
    public Number getBigPoolByRoomCfgId(int gameType, int roomCfgId){
        return (Number)redisTemplate.opsForHash().get(tableName(gameType),roomCfgId);
    }

    /**
     * 根据场次id获取池子
     * @param roomCfgId
     * @return
     */
    public Number getSmallPoolByRoomCfgId(int gameType, int roomCfgId){
        return (Number)redisTemplate.opsForHash().get(smallTableName(gameType),roomCfgId);
    }
    /**
     * 根据场次id获取池子
     * @param roomCfgId
     * @return
     */
    public Number getFakeSmallPoolByRoomCfgId(int gameType, int roomCfgId){
        return (Number)redisTemplate.opsForHash().get(fakeSmallTableName(gameType),roomCfgId);
    }

    /**
     * 池子加钱
     * @param roomCfgId
     * @param value
     * @return
     */
    public Long add(int gameType,int roomCfgId,long value){
        if(value < 1){
            log.debug("池子添加金币时，value不能小于0  gameType = {},roomCfgId = {},value = {}", gameType,roomCfgId, value);
            return null;
        }
        return redisTemplate.opsForHash().increment(tableName(gameType),roomCfgId,value);
    }

    /**
     * 池子减钱
     * @param roomCfgId
     * @param value
     * @return
     */
    public Long reduce(int gameType,int roomCfgId,long value){
        if(value > -1){
            log.debug("池子减少金币时，value不能大于-1  gameType = {},roomCfgId = {},value = {}", gameType,roomCfgId, value);
            return null;
        }

        long after = redisTemplate.opsForHash().increment(tableName(gameType),roomCfgId,value);
        if(after < 0){
            //如果减去value后，after为负数，则要回滚
            //因为池子减为负数的情况基本不可能，所以采用回滚方式，这样可以避免加锁带来的延迟
            redisTemplate.opsForHash().increment(tableName(gameType),roomCfgId,Math.abs(value));
            log.debug("池子减少金币后小于0，所以进行回滚  gameType = {},roomCfgId = {},value = {}", gameType,roomCfgId, value);
            return null;
        }
        return after;
    }

    public Map<Object, Object> getSmallPoolByRoomCfgId(int gameType) {
        // 直接获取整个Hash（因为只有3个字段，HGETALL最有效率）
        return redisTemplate.opsForHash().entries(smallTableName(gameType));
    }

    public Map<Object, Object> getFakeSmallPoolByRoomCfgId(int gameType) {
        return redisTemplate.opsForHash().entries(fakeSmallTableName(gameType));
    }

    protected String tableName(int gameType){
        return pool_prefix + gameType;
    }
    protected String smallTableName(int gameType){
        return small_pool_prefix + gameType;
    }
    protected String fakeSmallTableName(int gameType){
        return fake_small_pool_prefix + gameType;
    }
}
