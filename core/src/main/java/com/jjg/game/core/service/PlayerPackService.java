package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerPackDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

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
    @Autowired
    private ClusterSystem clusterSystem;

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
    public CommonResult<Void> addItems(long playerId, Map<Integer, Long> addItemMap, String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);
        long addGold = 0;
        long addDiamond = 0;
        Map<Integer, Long> addTempItemMap = new HashMap<>(addItemMap);
        Iterator<Map.Entry<Integer, Long>> it = addTempItemMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> next = it.next();
            int itemId = next.getKey();
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null) {
                it.remove();
                continue;
            }

            if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                addGold += Math.abs(next.getValue());
                it.remove();
                continue;
            }
            if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                addDiamond += Math.abs(next.getValue());
                it.remove();
            }
        }

        if (addGold > 0 || addDiamond > 0) {
            CommonResult<Player> goldAndDiamond = corePlayerService.addGoldAndDiamond(playerId, addGold, addDiamond, addType, true, null);
            if (!goldAndDiamond.success()) {
                result.code = goldAndDiamond.code;
                return result;
            }
            changeCurrencyAction(playerId, goldAndDiamond);
        }

        if (addTempItemMap.isEmpty()) {
            result.code = Code.SUCCESS;
            return result;
        }

        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                playerPack = new PlayerPack();
            }

            for (Map.Entry<Integer, Long> en : addTempItemMap.entrySet()) {
                int itemId = en.getKey();
                ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
                if (itemCfg == null) {
                    continue;
                }

                playerPack.addItem(itemId, en.getValue(), itemCfg.getProp());
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
        } catch (Exception e) {
            log.error("添加多个道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        if (result.success()) {
            coreLogger.addItems(playerId, addTempItemMap, addType);
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
    public CommonResult<Void> addItem(long playerId, int id, long count, String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);
        ItemCfg addItemCfg = GameDataManager.getItemCfg(id);
        //根据不同道具做不同处理
        if (addItemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
            CommonResult<Player> addResult = corePlayerService.addGold(playerId, count, addType);
            result.code = addResult.code;
            changeCurrencyAction(playerId, addResult);
            return result;
        } else if (addItemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
            CommonResult<Player> addResult = corePlayerService.addDiamond(playerId, count, addType);
            result.code = addResult.code;
            changeCurrencyAction(playerId, addResult);
            return result;
        }

        String key = getLockKey(playerId);
        int max = addItemCfg.getProp();

        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                playerPack = new PlayerPack();
            }

            playerPack.addItem(id, count, max);

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
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
    public CommonResult<Void> removeItem(long playerId, Item remove, String addType) {
        return removeItem(playerId, remove.getId(), remove.getCount(), null);
    }

    /**
     * 移除道具
     *
     * @param playerId
     * @param id
     * @param count
     * @return
     */
    public CommonResult<Void> removeItem(long playerId, int id, long count, String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);

        ItemCfg itemCfg = GameDataManager.getItemCfg(id);
        if (itemCfg == null) {
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {  //消耗金币
            CommonResult<Player> removeResult = corePlayerService.deductGold(playerId, count, addType);
            result.code = removeResult.code;
            changeCurrencyAction(playerId, removeResult);
            return result;
        } else if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {  //消耗钻石
            CommonResult<Player> removeResult = corePlayerService.deductDiamond(playerId, count, addType);
            result.code = removeResult.code;
            changeCurrencyAction(playerId, removeResult);
            return result;
        }

        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }

            CommonResult<Long> removeResult = playerPack.removeItem(id, count);
            if (!removeResult.success()) {
                result.code = removeResult.code;
                return result;
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            return result;
        } catch (Exception e) {
            log.error("移除道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }


    /**
     * 移除道具
     *
     * @param playerId
     * @param id
     * @param count
     * @return
     */
    public CommonResult<Void> removeItem(long playerId, int girdId, int id, long count, String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);

        ItemCfg itemCfg = GameDataManager.getItemCfg(id);
        if (itemCfg == null) {
            result.code = Code.NOT_FOUND;
            return result;
        }

        if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {  //消耗金币
            CommonResult<Player> removeResult = corePlayerService.deductGold(playerId, count, addType);
            result.code = removeResult.code;
            changeCurrencyAction(playerId, removeResult);
            return result;
        } else if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {  //消耗钻石
            CommonResult<Player> removeResult = corePlayerService.deductDiamond(playerId, count, addType);
            result.code = removeResult.code;
            changeCurrencyAction(playerId, removeResult);
            return result;
        }

        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }

            CommonResult<Long> removeResult = playerPack.removeItem(girdId, id, count);
            if (!removeResult.success()) {
                result.code = removeResult.code;
                return result;
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            return result;
        } catch (Exception e) {
            log.error("移除格子道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }


    private void changeCurrencyAction(long playerId, CommonResult<Player> result) {
        if (result.success()) {
            if (Objects.nonNull(result.data)) {
                PFSession session = clusterSystem.getSession(playerId);
                if (Objects.nonNull(session) && session.getReference() instanceof PlayerController playerController) {
                    playerController.setPlayer(result.data);
                }
            }
        }
    }

    /**
     * 移除道具
     *
     * @param playerId
     * @return
     */
    public CommonResult<Void> removeItems(long playerId, Map<Integer, Long> removeItemMap, String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);
        HashMap<Integer, Long> removeTempItemMap = new HashMap<>(removeItemMap);
        Iterator<Map.Entry<Integer, Long>> it = removeTempItemMap.entrySet().iterator();
        long deductGoldV = 0;
        long deductDiamondV = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, Long> next = it.next();
            int itemId = next.getKey();
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null) {
                it.remove();
                log.debug("移除道具失败，未找到配置 playerId = {},itemId = {}", playerId, itemId);
                continue;
            }

            //累加扣除金币
            if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                deductGoldV += Math.abs(next.getValue());
                it.remove();
                continue;
            }

            //扣除钻石
            if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                deductDiamondV += Math.abs(next.getValue());
                it.remove();
            }
        }

        //扣除金币和钻石
        if (deductGoldV > 0 || deductDiamondV > 0) {
            CommonResult<Player> removeResult = corePlayerService.deductGoldAndDiamond(playerId, deductGoldV, deductDiamondV, addType);
            if (!removeResult.success()) {
                result.code = removeResult.code;
                return result;
            }
            changeCurrencyAction(playerId, removeResult);
        }

        if (removeTempItemMap.isEmpty()) {
            result.code = Code.SUCCESS;
            return result;
        }

        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }
            for (Map.Entry<Integer, Long> entry : removeTempItemMap.entrySet()) {
                Integer id = entry.getKey();
                Long count = entry.getValue();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if (itemCfg == null) {
                    result.code = Code.NOT_FOUND;
                    return result;
                }
                CommonResult<Long> removeResult = playerPack.removeItem(id, count);
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            return result;
        } catch (Exception e) {
            log.error("移除道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }


    /**
     * 检查是否拥有道具
     *
     * @param player  玩家
     * @param itemMap 拥有道具
     * @return true 拥有 false 未拥有
     */
    public boolean checkHasItems(Player player, Map<Integer, Long> itemMap) {
        long playerId = player.getId();

        try {
            List<Item> itemList = new ArrayList<>();
            for (Map.Entry<Integer, Long> entry : itemMap.entrySet()) {
                itemList.add(new Item(entry.getKey(), entry.getValue()));
            }
            return checkHasItems(player, itemList);
        } catch (Exception e) {
            log.error("检查道具异常 失败 playerId={}", playerId, e);
        }
        return false;
    }

    /**
     * 检查是否拥有道具
     *
     * @param player   玩家
     * @param itemList 拥有道具
     * @return true 拥有 false 未拥有
     */
    public boolean checkHasItems(Player player, List<Item> itemList) {
        long playerId = player.getId();
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            for (Item item : itemList) {
                ItemCfg itemCfg = GameDataManager.getItemCfg(item.getId());
                if (Objects.isNull(itemCfg)) {
                    return false;
                }
                if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                    if (player.getGold() < item.getCount()) {
                        return false;
                    }
                    continue;
                }
                if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                    if (player.getDiamond() < item.getCount()) {
                        return false;
                    }
                    continue;
                }
                if (Objects.isNull(playerPack) || !playerPack.checkHasItems(List.of(item))) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("检查道具异常 playerId={}", playerId, e);
        }
        return false;
    }


    /**
     * 使用道具
     *
     * @param playerId
     * @param useItemId
     * @return
     */
    public CommonResult<Void> useItem(long playerId, int girdId, int useItemId, long useItemCount, Map<Integer, Long> addItemsMap,
                                        String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);

        CommonResult<Void> removeResult = removeItem(playerId, girdId, useItemId, useItemCount, addType);
        if (!removeResult.success()) {
            result.code = removeResult.code;
            return result;
        }

        CommonResult<Void> addResult = addItems(playerId, addItemsMap, addType);
        if (!addResult.success()) {
            //添加失败，要将之前扣除的道具加回去
            addItem(playerId, useItemId, useItemCount, "fail.rollback");
            result.code = addResult.code;
            log.debug("使用道具时，添加失败 playerId = {},girdId = {},useItemId = {}", playerId, girdId, useItemId);
            return result;
        }

        if (addResult.success()) {
            coreLogger.useItem(playerId, useItemId, 1, addType);
        }
        result.code = Code.SUCCESS;
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
