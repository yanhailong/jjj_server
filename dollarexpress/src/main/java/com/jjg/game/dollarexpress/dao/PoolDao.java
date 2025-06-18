package com.jjg.game.dollarexpress.dao;

import com.jjg.game.core.RedisLock;
import com.jjg.game.sample.DollarExpressWareHouseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/6/13 16:00
 */
@Repository
public class PoolDao {
    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String tableName = "dollarExpressPool";
    private static final String lockTableName = "dollarExpressPoolLock:";

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisLock redisLock;

    public void init(){
        for(DollarExpressWareHouseConfig config : DollarExpressWareHouseConfig.factory.getAllSamples()){
            redisTemplate.opsForHash().putIfAbsent(tableName,config.getSid(),config.basicWarehouse);
        }
    }

    private String getLockKey(int wareId) {
        return lockTableName + wareId;
    }

    /**
     * 根据场次id获取池子
     * @param wareId
     * @return
     */
    public Long getByWareId(int wareId){
        return (Long)redisTemplate.opsForHash().get(tableName,wareId);
    }

    /**
     * 池子加钱
     * @param wareId
     * @param value
     * @return
     */
    public Long add(int wareId,long value){
        if(value < 1){
            log.debug("池子添加金币时，value不能小于0  wareId = {},value = {}", wareId, value);
            return null;
        }
        return redisTemplate.opsForHash().increment(tableName,wareId,value);
    }

    /**
     * 池子减钱
     * @param wareId
     * @param value
     * @return
     */
    public Long reduce(int wareId,long value){
        if(value > -1){
            log.debug("池子减少金币时，value不能大于-1  wareId = {},value = {}", wareId, value);
            return null;
        }

        long after = redisTemplate.opsForHash().increment(tableName,wareId,value);
        if(after < 0){
            //如果减去value后，after为负数，则要回滚
            //因为池子减为负数的情况基本不可能，所以采用回滚方式，这样可以避免加锁带来的延迟
            redisTemplate.opsForHash().increment(lockTableName,wareId,Math.abs(value));
            log.debug("池子减少金币后小于0，所以进行回滚  wareId = {},value = {}", wareId, value);
            return null;
        }
        return after;
    }
}
