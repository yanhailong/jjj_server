package com.jjg.game.core.service;

import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerPackDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

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
     * @param addItemMap itemId -> count
     * @return
     */
    public CommonResult<PlayerPack> addItems(long playerId, Map<Integer, Long> addItemMap, String addType) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                playerPack = new PlayerPack();
            }

            long addGold = 0;
            long addDiamond = 0;
            for (Map.Entry<Integer, Long> en : addItemMap.entrySet()) {
                int itemId = en.getKey();
                ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
                if(itemCfg == null){
                    continue;
                }

                if(itemCfg.getType() == GameConstant.Item.TYPE_GOLD){
                    addGold += en.getValue();
                    continue;
                }
                if(itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND){
                    addDiamond += en.getValue();
                    continue;
                }
                playerPack.addItem(itemId, en.getValue(), itemCfg.getProp());
            }

            if(addGold > 0){
                corePlayerService.addGold(playerId, addGold,addType);
            }

            if(addDiamond > 0){
                corePlayerService.addGold(playerId, addDiamond,addType);
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            result.data = playerPack;
        } catch (Exception e) {
            log.error("添加多个道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        if (result.success()) {
            coreLogger.addItems(playerId, addItemMap, addType);
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
    public CommonResult<PlayerPack> addItem(long playerId, int id, long count, String addType) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);

        ItemCfg addItemCfg = GameDataManager.getItemCfg(id);
        int max = addItemCfg.getProp();

        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                playerPack = new PlayerPack();
            }

            //根据不同道具做不同处理
            if (addItemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                corePlayerService.addGold(playerId, count,addType);
            } else if (addItemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                corePlayerService.addDiamond(playerId, count,addType);
            } else {
                playerPack.addItem(id, count, max);
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            result.data = playerPack;
        } catch (Exception e) {
            log.error("添加道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        if (result.success()) {
            coreLogger.addItem(playerId, id, count, addType);
        }
        return result;
    }

    /**
     * 移除道具
     *
     * @param playerId 玩家id
     * @param remove   移除的道具
     * @return 最新的背包结果
     */
    public CommonResult<PlayerPack> removeItem(long playerId, Item remove,String addType) {
        return removeItem(playerId, remove.getId(), remove.getCount(),null);
    }

    /**
     * 移除道具
     *
     * @param playerId
     * @param id
     * @param count
     * @return
     */
    public CommonResult<PlayerPack> removeItem(long playerId, int id, long count,String addType) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);

        ItemCfg itemCfg = GameDataManager.getItemCfg(id);
        if(itemCfg == null){
            result.code = Code.NOT_FOUND;
            return result;
        }

        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }

            //根据不同道具做不同处理
            if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                CommonResult<Player> removeResult = corePlayerService.deductGold(playerId, count,addType);
                if(!removeResult.success()){
                    result.code = removeResult.code;
                    return result;
                }
            } else if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                CommonResult<Player> removeResult = corePlayerService.deductDiamond(playerId, count,addType);
                if(!removeResult.success()){
                    result.code = removeResult.code;
                    return result;
                }
            } else {
                CommonResult<Long> removeResult = playerPack.removeItem(id, count);
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
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
        return result;
    }

    /**
     * 使用道具
     *
     * @param playerId
     * @param useItemId
     * @param addItemId
     * @param addItemCount
     * @return
     */
    public CommonResult<PlayerPack> useItem(long playerId, int useItemId, int addItemId, long addItemCount,
                                            String addType) {
        CommonResult<PlayerPack> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);

        int useItemPropMax = GameDataManager.getItemCfg(useItemId).getProp();

        ItemCfg addItemCfg = GameDataManager.getItemCfg(addItemId);
        int addItemMax = addItemCfg.getProp();
        int useCount = 1;
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            //获取背包数据
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }

            //移除道具
            CommonResult<Long> removeResult = playerPack.removeItem(useItemId, useCount);
            if (!removeResult.success()) {
                result.code = removeResult.code;
                return result;
            }

            //根据不同道具做不同处理
            if (addItemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                CommonResult<Player> addResult = corePlayerService.addGold(playerId, addItemCount,
                        useItemAddType, useItemId + "");
                if (!addResult.success()) {
                    //如果添加失败，要将道具添加回去
                    playerPack.addItem(useItemId, useCount, useItemPropMax);
                    result.code = addResult.code;
                    return result;
                }
            } else if (addItemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                CommonResult<Player> addResult = corePlayerService.addDiamond(playerId, addItemCount,
                        useItemAddType, useItemId + "");
                if (!addResult.success()) {
                    //如果添加失败，要将道具添加回去
                    playerPack.addItem(useItemId, useCount, useItemPropMax);
                    result.code = addResult.code;
                    return result;
                }
            } else {
                playerPack.addItem(addItemId, (int) addItemCount, addItemMax);
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            result.data = playerPack;
            return result;
        } catch (Exception e) {
            log.error("使用道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        if (result.success()) {
            coreLogger.useItem(playerId, useItemId, 1, addType);
        }
        return result;
    }

    public PlayerPack checkAndSave(long playerId, DataSaveCallback<PlayerPack> cbk) {
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = redisGet(playerId);
            if (playerPack == null) {
                return null;
            }

            //如果执行失败
            if (!(boolean) cbk.updateDataWithRes(playerPack)) {
                return null;
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            return playerPack;
        } catch (Exception e) {
            log.error("保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return null;
    }

    public PlayerPack doSave(long playerId, DataSaveCallback<PlayerPack> cbk) {
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
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
