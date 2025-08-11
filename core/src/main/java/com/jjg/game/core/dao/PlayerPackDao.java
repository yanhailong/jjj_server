package com.jjg.game.core.dao;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author 11
 * @date 2025/8/7 15:14
 */
@Repository
public class PlayerPackDao extends MongoBaseDao<PlayerPack,Long>{
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public PlayerPackDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerPack.class, mongoTemplate);
    }

    private final String tableName = "playerPack";
    private final String lockTableName = "lockplayerpack:";

    @Autowired
    private RedisTemplate<String, PlayerPack> redisTemplate;
    @Autowired
    private RedisLock redisLock;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }


    public PlayerPack checkAndSave(long playerId, DataSaveCallback<PlayerPack> cbk) {
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    PlayerPack playerPack = redisGet(playerId);
                    if (playerPack == null) {
                        return null;
                    }

                    //如果执行失败
                    if (!(boolean) cbk.updateDataWithRes(playerPack)) {
                        break;
                    }

                    redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                    return playerPack;
                } catch (Exception e) {
                    log.error("保存 playerPack 失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.error("保存 playerPack 数据失败出现异常,playerId = {}", playerId, e);
            }

        }
        return null;
    }

    public PlayerPack doSave(long playerId, DataSaveCallback<PlayerPack> cbk) {
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    PlayerPack playerPack = redisGet(playerId);
                    // 找不到的玩家或者机器人玩家不保存数据
                    if (playerPack == null) {
                        return null;
                    }
                    //如果执行失败
                    cbk.updateData(playerPack);
                    redisSave(playerPack);
                    return playerPack;
                } catch (Exception e) {
                    log.warn("保存 playerPack 失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                log.warn("保存 playerPack 数据失败出现异常44,playerId = " + playerId, e);
            }
        }
        return null;
    }

    /**
     * 通过玩家ID获取玩家背包
     * @param playerId 玩家ID
     * @return 玩家背包对象
     */
    public PlayerPack redisGet(long playerId) {
        HashOperations<String, String, PlayerPack> operations = redisTemplate.opsForHash();
        return operations.get(tableName, playerId);
    }

    /**
     * 直接覆盖保存
     * @param playerPack
     */
    public void redisSave(PlayerPack playerPack) {
        redisTemplate.opsForHash().put(tableName, playerPack.getPlayerId(), playerPack);
    }

    public void redisDel(long playerId){
        redisTemplate.opsForHash().delete(tableName, playerId);
    }

    /**
     * 查询 PlayerPack 对象
     * 先查询redis
     * 再查询mongodb
     *
     * @param playerId
     * @return
     */
    public PlayerPack getFromAllDB(long playerId) {
        PlayerPack playerPack = redisGet(playerId);
        if (playerPack != null) {
            return playerPack;
        }

        Optional<PlayerPack> optional = findById(playerId);
        return optional.orElse(null);
    }
}
