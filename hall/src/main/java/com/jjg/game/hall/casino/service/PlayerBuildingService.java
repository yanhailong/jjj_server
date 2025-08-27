package com.jjg.game.hall.casino.service;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.hall.casino.dao.PlayerBuildingDao;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerBuildingService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final String tableName = "playerBuilding:";

    private final RedisTemplate<String, PlayerBuilding> redisTemplate;

    private final PlayerBuildingDao playerBuildingDao;

    public PlayerBuildingService(@Autowired RedisTemplate<String, PlayerBuilding> redisTemplate,
                                 @Autowired PlayerBuildingDao playerBuildingDao) {
        this.redisTemplate = redisTemplate;
        this.playerBuildingDao = playerBuildingDao;
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
        return playerBuildingDao.findByPlayerIdAndCasinoId(playerId, casinoId);
    }

    /**
     * 持久化到mongodb
     *
     * @param playerId 玩家id
     */
    public void moveToMongo(long playerId) {
        try {
            List<PlayerBuilding> playerBuilding = getFromAllDB(playerId);
            if (playerBuilding == null) {
                return;
            }
            playerBuildingDao.saveAll(playerBuilding);
            redisDel(playerId);
        } catch (Exception e) {
            log.error("保存到mongo失败 playerId:{}", playerId);
        }
    }

    private String getKey(long playerId) {
        return tableName + playerId;
    }


    /**
     * 保存整个对象
     */
    public void redisSave(PlayerBuilding playerBuilding) {
        redisTemplate.opsForHash().put(getKey(playerBuilding.getPlayerId()), playerBuilding.getCasinoId(), playerBuilding);
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
