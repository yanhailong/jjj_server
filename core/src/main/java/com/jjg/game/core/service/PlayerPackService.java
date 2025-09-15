package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.item.EItemUseStrategy;
import com.jjg.game.core.base.player.IPlayerRegister;
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
    @Autowired
    private ClusterSystem clusterSystem;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }


    /**
     * 添加道具
     */
    public CommonResult<Long> addItem(long playerId, int id, long count, String addType) {
        return addItems(playerId, Collections.singletonList(new Item(id, count)), addType);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<Long> addItems(long playerId, Map<Integer, Long> addItemMap, String addType) {
        List<Item> itemList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : addItemMap.entrySet()) {
            itemList.add(new Item(entry.getKey(), entry.getValue()));
        }
        return addItems(playerId, itemList, addType);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<Long> addItems(long playerId, List<Item> addItemList, String addType) {
        CommonResult<Long> result = new CommonResult<>(Code.FAIL);
        long addGold = 0;
        long addDiamond = 0;
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
        }

        if (itemList.isEmpty()) {
            result.code = Code.SUCCESS;
            return result;
        }

        String key = getLockKey(playerId);
        Map<EItemUseStrategy, List<Item>> autoActivateMap = new HashMap<>(itemList.size());
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            if (playerPack == null) {
                playerPack = new PlayerPack();
            }

            for (Item item : itemList) {
                int itemId = item.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
                if (itemCfg == null) {
                    continue;
                }
                EItemUseStrategy strategy = EItemUseStrategy.getItemUseStrategy(itemCfg.getType());
                if (strategy != null) {
                    autoActivateMap.computeIfAbsent(strategy, k -> new ArrayList<>()).add(item);
                } else {
                    playerPack.addItem(itemId, item.getItemCount(), itemCfg.getProp());
                }
            }

            redisTemplate.opsForHash().put(tableName, playerId, playerPack);
            result.code = Code.SUCCESS;
        } catch (Exception e) {
            log.error("添加多个道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        if (result.success()) {
            if (!autoActivateMap.isEmpty()) {
                for (Map.Entry<EItemUseStrategy, List<Item>> entry : autoActivateMap.entrySet()) {
                    entry.getKey().getUseStrategy().autoUse(playerId, entry.getValue());
                }
            }
            Map<Integer, Long> addTempItemMap =
                itemList.stream().collect(HashMap::new, (map, e) -> map.put(e.getId(), e.getItemCount()),
                    HashMap::putAll);
            result.data = coreLogger.addItems(playerId, addTempItemMap, addType);
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
    public CommonResult<Long> removeItem(long playerId, Item remove, String addType) {
        return removeItem(playerId, remove.getId(), remove.getItemCount(), addType);
    }

    /**
     * 移除道具
     */
    public CommonResult<Long> removeItem(long playerId, int id, long count, String addType) {
        return removeItem(playerId, null, id, count, addType);
    }

    /**
     * 移除道具
     */
    public CommonResult<Long> removeItem(long playerId, Integer girdId, int id, long count, String addType) {
        Player player = corePlayerService.get(playerId);
        return removeItem(player, Collections.singletonList(new Item(girdId, id, count)), addType);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<Long> removeItems(Player player, Map<Integer, Long> removeItemMap, String addType) {
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
    public CommonResult<Long> removeItem(Player player, List<Item> removeItemList, String addType) {
        CommonResult<Long> result = new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        int code = checkHasItems(player, removeItemList);
        if (code != Code.SUCCESS) {
            result.code = code;
            return result;
        }
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
                if (gridId == null) {
                    removeResult = playerPack.removeItem(id, count);
                } else {
                    removeResult = playerPack.removeItem(gridId, id, count);
                }
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
            }
            //扣除金币和钻石
            if (deductGoldV > 0 || deductDiamondV > 0) {
                CommonResult<Player> removeResult =
                        corePlayerService.deductGoldAndDiamond(playerId, deductGoldV, deductDiamondV, addType);
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
            }
            if (packItemList.isEmpty()) {
                result.code = Code.SUCCESS;
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
    public CommonResult<Void> useItem(long playerId, int girdId, int useItemId, long useItemCount,
                                      Map<Integer, Long> addItemsMap,
                                      String addType) {
        CommonResult<Void> result = new CommonResult<>(Code.FAIL);

        CommonResult<Long> removeResult = removeItem(playerId, girdId, useItemId, useItemCount, addType);
        if (!removeResult.success()) {
            result.code = removeResult.code;
            return result;
        }

        CommonResult<Long> addResult = addItems(playerId, addItemsMap, addType);
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

    @Override
    public void playerRegister(Player player) {
        PlayerPack pack = new PlayerPack();
        pack.setPlayerId(player.getId());
        redisSave(pack);
    }
}
