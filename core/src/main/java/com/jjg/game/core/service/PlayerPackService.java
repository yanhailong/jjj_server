package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.item.EItemUseStrategy;
import com.jjg.game.core.base.player.IPlayerRegister;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.PlayerPackDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.TaskConditionParam12101;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerPackService implements IPlayerRegister {
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
    @Lazy
    @Autowired
    private TaskManager taskManager;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }

    /**
     * 添加道具
     */
    public CommonResult<ItemOperationResult> addItem(long playerId, int id, long count, AddType addType) {
        return addItems(playerId, Collections.singletonList(new Item(id, count)), addType, null);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, Map<Integer, Long> addItemMap, AddType addType) {
        return addItems(playerId, addItemMap, addType, null);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, Map<Integer, Long> addItemMap, AddType addType, String desc) {
        List<Item> itemList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : addItemMap.entrySet()) {
            itemList.add(new Item(entry.getKey(), entry.getValue()));
        }
        return addItems(playerId, itemList, addType, desc);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, List<Item> addItemList, AddType addType) {
        return addItems(playerId, addItemList, addType, null);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, List<Item> addItemList, AddType addType, String desc) {
        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.FAIL);
        long addGold = 0;
        long addDiamond = 0;
        result.data = new ItemOperationResult();
        List<Item> itemList = new ArrayList<>();
        for (Item item : addItemList) {
            int itemId = item.getId();
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null) {
                continue;
            }
            if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                addGold += Math.abs(item.getItemCount());
                continue;
            }
            if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                addDiamond += Math.abs(item.getItemCount());
                continue;
            }
            itemList.add(item);
        }

        if (addGold > 0 || addDiamond > 0) {
            CommonResult<Player> goldAndDiamond =
                    corePlayerService.addGoldAndDiamond(playerId, addGold, addDiamond, addType, true, null);
            if (!goldAndDiamond.success()) {
                result.code = goldAndDiamond.code;
                return result;
            }
            result.data.setDiamond(goldAndDiamond.data.getDiamond());
            result.data.setGoldNum(goldAndDiamond.data.getGold());
        }

        if (itemList.isEmpty()) {
            result.code = Code.SUCCESS;
            return result;
        }
        PlayerPack playerPack = null;
        String key = getLockKey(playerId);
        // TODO 当前的加锁位置会有数据覆盖问题
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                playerPack = new PlayerPack(playerId);
            }
            boolean hasChange = false;
            Map<Integer, Long> changeBefore = new HashMap<>(itemList.size());
            for (Item item : itemList) {
                int itemId = item.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
                if (itemCfg == null) {
                    continue;
                }
                EItemUseStrategy strategy = EItemUseStrategy.getItemUseStrategy(itemCfg.getType());
                int useNum = 0;
                if (strategy != null) {
                    //尝试自动使用
                    useNum = strategy.getUseStrategy().autoUse(playerId, item, itemCfg);
                }
                if (item.getItemCount() - useNum > 0) {
                    changeBefore.put(itemId, changeBefore.getOrDefault(itemId, 0L) + playerPack.getItemCount(itemId));
                    playerPack.addItem(itemId, item.getItemCount() - useNum, itemCfg.getProp());
                    hasChange = true;
                }
            }
            // 如果有改变才写入，自使用的道具不会改变背包数据
            if (hasChange) {
                redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                result.data.setChangeBeforeItemNum(changeBefore);
            }
            result.code = Code.SUCCESS;
        } catch (Exception e) {
            log.error("添加多个道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        if (result.success()) {
            if (playerPack != null) {
                Map<Integer, Long> changeAfterNum = new HashMap<>(itemList.size());
                for (Item item : itemList) {
                    long count = playerPack.getItemCount(item.getId());
                    changeAfterNum.put(item.getId(), count);
                }
                result.data.setChangeEndItemNum(changeAfterNum);
            }
            Map<Integer, Long> addTempItemMap =
                    itemList.stream().collect(HashMap::new, (map, e) -> map.put(e.getId(), e.getItemCount()),
                            HashMap::putAll);
            coreLogger.addItems(playerId, addTempItemMap, addType, desc);
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
    public CommonResult<ItemOperationResult> removeItem(long playerId, Item remove, AddType addType) {
        return removeItem(playerId, remove.getId(), remove.getItemCount(), addType);
    }

    /**
     * 移除道具
     */
    public CommonResult<ItemOperationResult> removeItem(long playerId, int id, long count, AddType addType) {
        return removeItem(playerId, null, id, count, addType);
    }

    /**
     * 移除道具
     */
    public CommonResult<ItemOperationResult> removeItem(long playerId, Integer girdId, int id, long count,
                                                        AddType addType) {
        Player player = corePlayerService.get(playerId);
        return removeItem(player, Collections.singletonList(new Item(girdId, id, count)), addType);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<ItemOperationResult> removeItems(Player player, Map<Integer, Long> removeItemMap,
                                                         AddType addType) {
        List<Item> itemList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : removeItemMap.entrySet()) {
            itemList.add(new Item(entry.getKey(), entry.getValue()));
        }
        return removeItem(player, itemList, addType);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<ItemOperationResult> removeItem(Player player, List<Item> removeItemList, AddType addType) {
        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        int code = checkHasItems(player, removeItemList);
        if (code != Code.SUCCESS) {
            result.code = code;
            return result;
        }
        result.data = new ItemOperationResult();
        long deductGoldV = 0;
        long deductDiamondV = 0;
        long playerId = player.getId();
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            List<Item> packItemList = new ArrayList<>();
            for (Item item : removeItemList) {
                int itemId = item.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
                if (itemCfg == null) {
                    log.debug("移除道具失败，未找到配置 playerId = {},itemId = {}", playerId, itemId);
                    continue;
                }
                //扣除钻石
                if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                    deductDiamondV += Math.abs(item.getItemCount());
                    continue;
                }
                //累加扣除金币
                if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                    deductGoldV += Math.abs(item.getItemCount());
                    continue;
                }
                packItemList.add(item);
            }
            boolean hasGoldAndDiamond = deductDiamondV > 0 || deductGoldV > 0;
            // 如果道具列表为空，但是还需要处理金币钻石，继续处理
            if (packItemList.isEmpty() && !hasGoldAndDiamond) {
                result.code = Code.SUCCESS;
                return result;
            }

            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }

            //检查道具
            if (!playerPack.checkHasItems(packItemList)) {
                result.code = Code.NOT_ENOUGH_ITEM;
                return result;
            }

            Map<Integer, Long> changeBefore = new HashMap<>(packItemList.size());
            for (Item item : packItemList) {
                int id = item.getId();
                long count = item.getItemCount();
                Integer gridId = item.getGridId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if (itemCfg == null) {
                    result.code = Code.NOT_FOUND;
                    return result;
                }
                CommonResult<Long> removeResult;
                changeBefore.put(id, changeBefore.getOrDefault(id, 0L) + playerPack.getItemCount(id));
                if (gridId == null) {
                    removeResult = playerPack.removeItem(id, count);
                } else {
                    removeResult = playerPack.removeItem(gridId, id, count);
                }
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
                Thread.ofVirtual().start(() -> {
                    //触发消耗道具任务
                    taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> {
                        TaskConditionParam12101 param = new TaskConditionParam12101();
                        param.setItemId(id);
                        param.setAddValue(count);
                        return param;
                    });
                });
            }
            //扣除金币和钻石
            if (deductGoldV > 0 || deductDiamondV > 0) {
                CommonResult<Player> removeResult =
                        corePlayerService.deductGoldAndDiamond(playerId, deductGoldV, deductDiamondV, addType);
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
                result.data.setDiamond(removeResult.data.getDiamond());
                result.data.setGoldNum(removeResult.data.getGold());
                long finalDeductGoldV = deductGoldV;
                long finalDeductDiamondV = deductDiamondV;
                Thread.ofVirtual().start(() -> {
                    //触发消耗金币任务
                    if (finalDeductGoldV > 0) {
                        TaskConditionParam12101 param = new TaskConditionParam12101();
                        param.setItemId(ItemUtils.getGoldItemId());
                        param.setAddValue(finalDeductGoldV);
                        param.setResultValue(removeResult.data.getGold());
                        taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> param);
                    }
                    //触发消耗钻石任务
                    if (finalDeductDiamondV > 0) {
                        TaskConditionParam12101 param = new TaskConditionParam12101();
                        param.setItemId(ItemUtils.getDiamondItemId());
                        param.setAddValue(finalDeductDiamondV);
                        param.setResultValue(removeResult.data.getDiamond());
                        taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> param);
                    }
                });
            }
            if (packItemList.isEmpty()) {
                result.code = Code.SUCCESS;
                return result;
            }
            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
            //放入最新的道具信息
            Map<Integer, Long> changeAfterNum = new HashMap<>();
            for (Item item : packItemList) {
                changeAfterNum.put(item.getId(), playerPack.getItemCount(item.getId()));
            }
            result.data.setChangeEndItemNum(changeAfterNum);
            result.data.setChangeBeforeItemNum(changeBefore);
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
            return checkHasItems(player, itemList) == Code.SUCCESS;
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
    public int checkHasItems(Player player, List<Item> itemList) {
        long playerId = player.getId();
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            for (Item item : itemList) {
                ItemCfg itemCfg = GameDataManager.getItemCfg(item.getId());
                if (Objects.isNull(itemCfg)) {
                    return Code.NOT_FOUND;
                }
                if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                    if (player.getGold() < item.getItemCount()) {
                        return Code.NOT_ENOUGH;
                    }
                    continue;
                }
                if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                    if (player.getDiamond() < item.getItemCount()) {
                        return Code.NOT_ENOUGH;
                    }
                    continue;
                }
                if (Objects.isNull(playerPack) || !playerPack.checkHasItems(List.of(item))) {
                    return Code.NOT_ENOUGH_ITEM;
                }
            }
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("检查道具异常 playerId={}", playerId, e);
        }
        return Code.FAIL;
    }

    /**
     * 使用道具
     *
     * @param playerId
     * @param useItemId
     * @return
     */
    public CommonResult<ItemOperationResult> useItem(long playerId, int useItemId, long useItemCount,
                                                     Map<Integer, Long> addItemsMap,
                                                     AddType addType) {
        return useItem(playerId, null, useItemId, useItemCount, addItemsMap, addType);
    }

    /**
     * 使用道具
     *
     * @param playerId
     * @param useItemId
     * @return
     */
    public CommonResult<ItemOperationResult> useItem(long playerId, Integer girdId, int useItemId, long useItemCount,
                                                     Map<Integer, Long> addItemsMap,
                                                     AddType addType) {
        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.FAIL);

        CommonResult<ItemOperationResult> removeResult = removeItem(playerId, girdId, useItemId, useItemCount, addType);
        if (!removeResult.success()) {
            result.code = removeResult.code;
            return result;
        }

        CommonResult<ItemOperationResult> addResult = addItems(playerId, addItemsMap, addType);
        if (!addResult.success()) {
            //添加失败，要将之前扣除的道具加回去
            addItem(playerId, useItemId, useItemCount, AddType.FAIL_ROLLBACK);
            result.code = addResult.code;
            log.debug("使用道具时，添加失败 playerId = {},girdId = {},useItemId = {}", playerId, girdId, useItemId);
            return result;
        }

        if (addResult.success()) {
            coreLogger.useItem(playerId, useItemId, 1, addType);
        }
        result.code = Code.SUCCESS;
        result.data = addResult.data;
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

    @Override
    public void playerRegister(Player player) {
        PlayerPack pack = new PlayerPack(player.getId());
        redisSave(pack);
    }
}
