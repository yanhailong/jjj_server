package com.jjg.game.hall.casino.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jjg.game.common.redis.RedisJsonTemplate;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.hall.casino.dao.PlayerBuildingDao;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author 11
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerBuildingService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final String tableName = "player:building:";
    private final String lockTableName = "lock:" + tableName;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private RedisJsonTemplate redisJsonTemplate;
    @Autowired
    private PlayerBuildingDao playerBuildingDao;
    @Autowired
    private RedisLock redisLock;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }


    /**
     * 更新赌场信息
     */
    public <T> CommonResult<T> updateData(long playerId, Consumer<CommonResult<T>> callback) {
        CommonResult<T> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            callback.accept(result);
        } catch (Exception e) {
            log.error("玩家更新赌场信息，修改数据 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }


    /**
     * 查询 PlayerBuilding 对象
     * 先查询redis
     * 再查询mongodb
     *
     * @param playerId 玩家id
     * @return 玩家建筑数据
     */
    public PlayerBuilding getFromAllDB(long playerId) {
        PlayerBuilding playerBuilding = redisGet(playerId);
        if (playerBuilding != null) {
            return playerBuilding;
        }
        //TODO保存到redis
        return playerBuildingDao.findById(playerId);
    }

    /**
     * 持久化到mongodb
     *
     * @param playerId 玩家id
     */
    public void moveToMongo(long playerId) {
        PlayerBuilding playerBuilding = redisGet(playerId);
        if (playerBuilding == null) {
            return;
        }
        playerBuildingDao.save(playerBuilding);
        redisDel(playerBuilding.getPlayerId());
    }

    private String getKey(long playerId) {
        return tableName + playerId;
    }

    /**
     * 保存整个对象
     */
    public void redisSave(PlayerBuilding playerBuilding) {
        redisJsonTemplate.set(getKey(playerBuilding.getPlayerId()), playerBuilding);
    }

    /**
     * 删除整个对象
     */
    public void redisDel(long playerId) {
        redisTemplate.delete(getKey(playerId));
    }

    /**
     * 通过玩家ID获取玩家建筑信息
     *
     * @param playerId 玩家ID
     * @return 玩家建筑信息
     */
    public PlayerBuilding redisGet(long playerId) {
        return redisJsonTemplate.get(getKey(playerId), new TypeReference<>() {
        });
    }

    /**
     * 根据JSONPath获取数据
     */
    public <K> K getDataOfPath(long playerId, String path, TypeReference<K> typeRef) {
        return redisJsonTemplate.getPath(getKey(playerId), path, typeRef);
    }

    /**
     * 根据JSONPath设置数据
     */
    public <K> void setDataOfPath(long playerId, String path, K k) {
        redisJsonTemplate.setPath(getKey(playerId), path, k);
    }


}
