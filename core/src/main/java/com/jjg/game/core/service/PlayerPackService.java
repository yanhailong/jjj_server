package com.jjg.game.core.service;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.core.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerPackDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.logger.CoreLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerPackService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final String tableName = "playerPack";
    private final String lockTableName = "lockplayerpack:";

    private final String useItemAddType = "useItem";

    @Autowired
    private RedisTemplate<String, PlayerPack> redisTemplate;
    @Autowired
    private PlayerPackDao playerPackDao;
    @Autowired
    private RedisLock redisLock;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private CoreLogger coreLogger;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }

    /**
     * 添加多个道具
     *
     * @param playerId
     * @param playerId
     * @param list  arr -> 0.itemId  1.count  2.max
     * @return
     */
    public CommonResult<PlayerPack> addItems(long playerId, List<int[]> list) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    PlayerPack playerPack = getFromAllDB(playerId);
                    if (playerPack == null) {
                        playerPack = new PlayerPack();
                    }

                    for(int[] arr : list) {
                        playerPack.addItem(arr[0], arr[1], arr[2]);
                    }

                    redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                    result.code = Code.SUCCESS;
                    result.data = playerPack;
                    return result;
                } catch (Exception e) {
                    log.error("添加多个道具，保存 playerPack 失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.error("添加多个道具，保存 playerPack 数据失败出现异常,playerId = {}", playerId, e);
            }
        }
        return result;
    }

    /**
     * 添加道具
     *
     * @param playerId
     * @param id
     * @param count
     * @return
     */
    public CommonResult<PlayerPack> addItem(long playerId, int id, int count, int max) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    PlayerPack playerPack = getFromAllDB(playerId);
                    if (playerPack == null) {
                        playerPack = new PlayerPack();
                    }

                    playerPack.addItem(id, count, max);

                    redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                    result.code = Code.SUCCESS;
                    result.data = playerPack;
                    return result;
                } catch (Exception e) {
                    log.error("添加道具，保存 playerPack 失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.error("添加道具，保存 playerPack 数据失败出现异常,playerId = {}", playerId, e);
            }

        }
        return result;
    }

    /**
     * 添加道具
     *
     * @param playerId
     * @param id
     * @param count
     * @return
     */
    public CommonResult<PlayerPack> removeItem(long playerId, int id, int count) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    PlayerPack playerPack = getFromAllDB(playerId);
                    if (playerPack == null) {
                        result.code = Code.NOT_FOUND;
                        return result;
                    }

                    CommonResult<Integer> removeResult = playerPack.removeItem(id, count);
                    if (!removeResult.success()) {
                        result.code = removeResult.code;
                        return result;
                    }

                    redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                    result.code = Code.SUCCESS;
                    result.data = playerPack;
                    return result;
                } catch (Exception e) {
                    log.error("移除道具，保存 playerPack 失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.error("移除道具，保存 playerPack 数据失败出现异常,playerId = {}", playerId, e);
            }

        }
        return result;
    }

    /**
     * 使用道具
     *
     * @param playerId
     * @param id
     * @param addItemId
     * @param addItemCount
     * @param addItemMax
     * @return
     */
    public CommonResult<PlayerPack> useItem(long playerId, int id,int useItemPropMax, int addItemId, long addItemCount, int addItemMax,String addType) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        for (int i = 0; i < GameConstant.Common.REDIS_TRANSACTION_TRY_COUNT; i++) {
            if (redisLock.lock(key)) {
                try {
                    //获取背包数据
                    PlayerPack playerPack = getFromAllDB(playerId);
                    if (playerPack == null) {
                        result.code = Code.NOT_FOUND;
                        return result;
                    }

                    //移除道具
                    CommonResult<Integer> removeResult = playerPack.removeItem(id, 1);
                    if (!removeResult.success()) {
                        result.code = removeResult.code;
                        return result;
                    }

                    //根据不同道具做不同处理
                    if (addItemId == GameConstant.Item.ID_GOLD) {
                        CommonResult<Player> addResult = corePlayerService.addGold(playerId, addItemCount, useItemAddType, id + "");
                        if(!addResult.success()) {
                            //如果添加失败，要将道具添加回去
                            playerPack.addItem(id,1,useItemPropMax);
                            break;
                        }
                    } else if (addItemId == GameConstant.Item.ID_DIAMOND) {
                        CommonResult<Player> addResult = corePlayerService.addDiamond(playerId, addItemCount, useItemAddType, id + "");
                        if(!addResult.success()) {
                            //如果添加失败，要将道具添加回去
                            playerPack.addItem(id,1,useItemPropMax);
                            break;
                        }
                    } else {
                        playerPack.addItem(addItemId, (int) addItemCount, addItemMax);
                    }

                    redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                    result.code = Code.SUCCESS;
                    result.data = playerPack;
                    break;
                } catch (Exception e) {
                    log.error("使用道具，保存 playerPack 失败 playerId={}", playerId, e);
                } finally {
                    redisLock.unlock(key);
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                log.error("使用道具，保存 playerPack 数据失败出现异常,playerId = {}", playerId, e);
            }
        }

        if(result.success()){
            coreLogger.useItem(playerId,id,1,addType);
        }
        return result;
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
     *
     * @param playerId 玩家ID
     * @return 玩家背包对象
     */
    public PlayerPack redisGet(long playerId) {
        HashOperations<String, String, PlayerPack> operations = redisTemplate.opsForHash();
        return operations.get(tableName, playerId);
    }

    /**
     * 直接覆盖保存
     *
     * @param playerPack
     */
    public void redisSave(PlayerPack playerPack) {
        redisTemplate.opsForHash().put(tableName, playerPack.getPlayerId(), playerPack);
    }

    public void redisDel(long playerId) {
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

        return playerPackDao.findById(playerId);
    }

    /**
     * 持久化到mongodb
     *
     * @param playerId
     */
    public void moveToMongo(long playerId) {
        PlayerPack playerPack = redisGet(playerId);
        if (playerPack == null) {
            return;
        }
        playerPackDao.save(playerPack);
        redisDel(playerPack.getPlayerId());
    }
}
