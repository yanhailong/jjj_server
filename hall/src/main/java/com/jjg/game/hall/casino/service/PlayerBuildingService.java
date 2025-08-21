package com.jjg.game.hall.casino.service;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.hall.casino.dao.PlayerBuildingDao;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author 11
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerBuildingService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final String tableName = "playerBuilding:";
    private final String lockTableName = "lock:" + tableName;

    @Autowired
    private RedisTemplate<String, PlayerBuilding> redisTemplate;
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
    public <K> CommonResult<K> updateData(long playerId, int casinoId, BiConsumer<CommonResult<K>, PlayerBuilding> callback) {
        CommonResult<K> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            //获取赌场信息
            PlayerBuilding playerBuilding = getCasinoInfoFromAllDB(playerId, casinoId);
            if (Objects.isNull(playerBuilding)) {
                result.code = Code.PARAM_ERROR;
                return result;
            }
            callback.accept(result, playerBuilding);
            //回存数据
            if (result.code == Code.SUCCESS) {
                redisSave(playerId, casinoId, playerBuilding);
            }
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
    public List<PlayerBuilding> getFromAllDB(long playerId) {
        List<PlayerBuilding> playerBuildings = redisGetAll(playerId);
        if (playerBuildings != null) {
            return playerBuildings;
        }
        //TODO保存到redis
        return playerBuildingDao.findByPlayerId(playerId);
    }

    /**
     * 查询 PlayerBuilding 对象
     * 先查询redis
     * 再查询mongodb
     *
     * @param playerId 玩家id
     * @return 玩家建筑数据
     */
    public PlayerBuilding getCasinoInfoFromAllDB(long playerId, int casinoId) {
        PlayerBuilding playerBuilding = redisGet(playerId, casinoId);
        if (playerBuilding != null) {
            return playerBuilding;
        }
        //TODO保存到redis
        return playerBuildingDao.findByPlayerIdAndCasinoId(playerId, casinoId);
    }

    /**
     * 持久化到mongodb
     *
     * @param playerId 玩家id
     */
    public void moveToMongo(long playerId) {
        List<PlayerBuilding> playerBuilding = getFromAllDB(playerId);
        if (playerBuilding == null) {
            return;
        }
        playerBuildingDao.saveAll(playerBuilding);
        redisDel(playerId);
    }

    private String getKey(long playerId) {
        return tableName + playerId;
    }

    /**
     * 保存整个对象
     */
    public void redisSave(long playerId, int casinoId, PlayerBuilding playerBuilding) {
        redisTemplate.opsForHash().put(getKey(playerId), casinoId, playerBuilding);
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
    public List<PlayerBuilding> redisGetAll(long playerId) {
        HashOperations<String, Object, PlayerBuilding> hash = redisTemplate.opsForHash();
        return hash.values(getKey(playerId));
    }

    /**
     * 通过玩家ID获取玩家单个建筑信息
     *
     * @param playerId 玩家ID
     * @return 玩家建筑信息
     */
    public PlayerBuilding redisGet(long playerId, int casinoId) {
        HashOperations<String, Object, PlayerBuilding> hash = redisTemplate.opsForHash();
        return hash.get(getKey(playerId), casinoId);
    }


}
