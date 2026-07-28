package com.jjg.game.core.service;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.base.item.EItemUseStrategy;
import com.jjg.game.core.base.player.IPlayerRegister;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.PlayerPackDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.SpecialItemListener;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.listener.ItemAddListener;
import com.jjg.game.core.listener.ItemConsumeListener;
import com.jjg.game.core.pb.PackItemInfo;
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
    @Autowired(required = false)
    private List<ItemAddListener> itemAddListeners = Collections.emptyList();
    //一个道具只能由一个处理器承载，多个实现会在启动时直接冲突失败，好过静默取其一
    @Autowired(required = false)
    private SpecialItemListener specialItemListener;
    //@Lazy 打破循环: 本服务 -> 消费埋点实现 -> 任务服务 -> 本服务
    @Lazy
    @Autowired(required = false)
    private List<ItemConsumeListener> itemConsumeListeners = Collections.emptyList();

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
        return addItems(playerId, addItemMap, addType, true);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, Map<Integer, Long> addItemMap, AddType addType, boolean notify) {
        return addItems(playerId, addItemMap, addType, null, notify);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, Map<Integer, Long> addItemMap, AddType addType, String desc) {
        return addItems(playerId, addItemMap, addType, desc, true);
    }

    public CommonResult<ItemOperationResult> addItems(long playerId, Map<Integer, Long> addItemMap, AddType addType, String desc, boolean notify) {
        List<Item> itemList = checkItemParam(addItemMap);
        if (CollectionUtil.isEmpty(itemList)) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
            result.data = new ItemOperationResult();
            return result;
        }
        return addItems(playerId, itemList, addType, desc, notify);
    }

    public List<Item> checkItemParam(Map<Integer, Long> addItemMap) {
        if (CollectionUtil.isEmpty(addItemMap)) {
            return List.of();
        }
        List<Item> itemList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : addItemMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            itemList.add(new Item(entry.getKey(), entry.getValue()));
        }
        return itemList;
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
        return addItems(playerId, addItemList, addType, desc, true);
    }

    /**
     * 添加多个道具
     */
    public CommonResult<ItemOperationResult> addItems(long playerId, List<Item> addItemList, AddType addType, String desc, boolean notify) {
        if (CollectionUtil.isEmpty(addItemList)) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
            result.data = new ItemOperationResult();
            return result;
        }

        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.FAIL);
        result.data = new ItemOperationResult();

        List<Item> validAddItemList = new ArrayList<>(addItemList.size());
        for (Item item : addItemList) {
            if (item == null) {
                continue;
            }
            //入账只接受正数：货币分支取的是 Math.abs，负数会被当成正数发放
            if (item.getItemCount() <= 0) {
                log.warn("添加道具跳过非正数 playerId={},itemId={},count={},addType={},desc={}",
                        playerId, item.getId(), item.getItemCount(), addType, desc);
                continue;
            }
            validAddItemList.add(item);
        }
        if (validAddItemList.isEmpty()) {
            result.code = Code.SUCCESS;
            return result;
        }

        //优先入账特殊道具，剩余的常规道具再走货币与背包。
        //必须留在取锁前：特殊道具入账可能重新回调本服务（如赛季币入账触发段位奖励发放），
        //若落在锁内，内层会读到外层尚未写回的旧背包，两者的写入将互相覆盖。
        List<Item> normalAddItemList = new ArrayList<>(validAddItemList.size());
        List<Item> specialAddItemList = splitSpecialItems(validAddItemList, normalAddItemList);
        if (!specialAddItemList.isEmpty()
                && !specialItemListener.addItems(playerId, specialAddItemList, addType, desc, notify)) {
            log.error("特殊道具入账失败 playerId={},items={},addType={},desc={}", playerId, specialAddItemList, addType, desc);
            return result;
        }

        try {
            result = addNormalItems(playerId, normalAddItemList, validAddItemList, addType, desc, notify);
        } catch (Exception e) {
            //配置查询、纯货币路径的 addMoneyCoin 都在 addNormalItems 的内层 try 之外，
            //异常若穿透出去就跳过了下面的补偿，特殊资源会留在账上
            log.error("添加常规道具异常 playerId={},items={},addType={},desc={}",
                    playerId, normalAddItemList, addType, desc, e);
        }
        //常规道具或货币入账失败(含抛异常)时撤回已入账的特殊道具，否则调用方按失败重试会重复发放
        if (!result.success()) {
            rollbackAddedSpecialItems(playerId, specialAddItemList);
        }
        return result;
    }

    /**
     * 常规道具入账：货币走 {@link CorePlayerService}，其余进背包
     *
     * @param normalItems 已剔除特殊道具的常规道具
     * @param allItems    本次入账的全部道具（含特殊道具），仅用于入账通知
     */
    private CommonResult<ItemOperationResult> addNormalItems(long playerId, List<Item> normalItems, List<Item> allItems,
                                                             AddType addType, String desc, boolean notify) {
        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.FAIL);
        result.data = new ItemOperationResult();

        long addGold = 0;
        long addDiamond = 0;
        long addShell = 0;

        List<Item> itemList = new ArrayList<>();
        for (Item item : normalItems) {
            int itemId = item.getId();
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null) {
                log.error("添加道具失败，未找到道具配置 playerId={},itemId={},count={},addType={},desc={}",
                        playerId, itemId, item.getItemCount(), addType, desc);
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
            if (itemCfg.getType() == GameConstant.Item.TYPE_SHELL) {
                addShell += Math.abs(item.getItemCount());
                continue;
            }
            if (!itemCfg.getIsBag()) {
                continue;
            }
            itemList.add(item);
        }

        if (itemList.isEmpty()) {
            if (addGold > 0 || addDiamond > 0 || addShell > 0) {
                CommonResult<Player> goldAndDiamond =
                        corePlayerService.addMoneyCoin(playerId, addGold, addDiamond, addShell, addType, notify, desc);
                if (!goldAndDiamond.success()) {
                    result.code = goldAndDiamond.code;
                    return result;
                }
                result.data.goldChange(addGold, goldAndDiamond.data.getGold());
                result.data.diamondChange(addDiamond, goldAndDiamond.data.getDiamond());
                result.data.shellChange(addShell, goldAndDiamond.data.getShell());
            }
            result.code = Code.SUCCESS;
            notifyItemsAdded(playerId, allItems, addType);
            return result;
        }
        PlayerPack playerPack = null;
        String key = getLockKey(playerId);
        boolean lock = false;
        boolean currencyAdded = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                result.code = Code.FAIL;
                log.debug("获取锁失败 lockKey:{} playerId = {} ", key, playerId);
                return result;
            }
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
                    log.debug("未找到该道具配置 playerId = {},itemId = {}", playerId, itemId);
                    continue;
                }
                EItemUseStrategy strategy = EItemUseStrategy.getItemUseStrategy(itemCfg.getType());
                int useNum = 0;
                if (strategy != null) {
                    //尝试自动使用
                    useNum = strategy.getUseStrategy().autoUse(playerId, item, itemCfg);
                }
                if (item.getItemCount() - useNum > 0) {
                    changeBefore.putIfAbsent(itemId, playerPack.getItemCount(itemId));
                    playerPack.addItem(itemId, item.getItemCount() - useNum, itemCfg.getProp());
                    hasChange = true;
                }
            }
            if (addGold > 0 || addDiamond > 0 || addShell > 0) {
                CommonResult<Player> goldAndDiamond =
                        corePlayerService.addMoneyCoin(playerId, addGold, addDiamond, addShell, addType, notify, desc);
                if (!goldAndDiamond.success()) {
                    result.code = goldAndDiamond.code;
                    return result;
                }
                currencyAdded = true;
                result.data.goldChange(addGold, goldAndDiamond.data.getGold());
                result.data.diamondChange(addDiamond, goldAndDiamond.data.getDiamond());
                result.data.shellChange(addShell, goldAndDiamond.data.getShell());
            }
            // 如果有改变才写入，自使用的道具不会改变背包数据
            if (hasChange) {
                redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                result.data.setChangeBeforeItemNum(changeBefore);
            }
            result.code = Code.SUCCESS;
        } catch (Exception e) {
            if (currencyAdded) {
                rollbackAddedCurrency(playerId, addGold, addDiamond, addShell);
            }
            result.code = Code.FAIL;
            log.error("添加多个道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
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
                    itemList.stream().collect(HashMap::new, (map, e) -> map.merge(e.getId(), e.getItemCount(), Long::sum),
                            HashMap::putAll);
            coreLogger.addItems(playerId, result.data.getChangeBeforeItemNum(), addTempItemMap, result.data.getChangeEndItemNum(), addType, desc);
            notifyItemsAdded(playerId, allItems, addType);
        }
        return result;
    }

    /**
     * 撤回已入账的特殊道具
     * <p>
     * 只能退回数值：赛季币入账时可能已推进段位并发过段位奖励、勋章激活后不可撤销，这些连带影响撤不掉。
     * 因此无论数值是否退回成功都留痕 —— 回退成功不等于状态已还原。
     */
    private void rollbackAddedSpecialItems(long playerId, List<Item> addedItems) {
        if (addedItems.isEmpty()) {
            return;
        }
        boolean rolledBack = specialItemListener.removeItems(playerId, addedItems, AddType.FAIL_ROLLBACK,
                "PlayerPackService addItems rollback");
        log.error("添加道具失败，回滚特殊道具 playerId={},items={},数值回退={}（段位晋升、段位奖励、勋章激活等无法撤销，需人工核对）",
                playerId, addedItems, rolledBack ? "成功" : "失败");
    }

    /**
     * 回滚已扣除的特殊道具
     */
    private void rollbackRemovedSpecialItems(long playerId, List<Item> removedItems) {
        if (removedItems.isEmpty()) {
            return;
        }
        if (!specialItemListener.addItems(playerId, removedItems, AddType.FAIL_ROLLBACK,
                "PlayerPackService removeItem rollback", false)) {
            log.error("移除道具回滚特殊道具失败(需人工修复) playerId={},items={}", playerId, removedItems);
        }
    }

    /**
     * 拆出由处理器承载的特殊道具，其余常规道具收集到 normalItems
     */
    private List<Item> splitSpecialItems(List<Item> items, List<Item> normalItems) {
        if (specialItemListener == null) {
            normalItems.addAll(items);
            return List.of();
        }
        List<Item> specialItems = new ArrayList<>();
        for (Item item : items) {
            if (specialItemListener.support(item.getId())) {
                specialItems.add(item);
            } else {
                normalItems.add(item);
            }
        }
        return specialItems;
    }

    /**
     * 该道具是否由处理器承载
     */
    private boolean isSpecialItem(int itemId) {
        return specialItemListener != null && specialItemListener.support(itemId);
    }

    private void notifyItemsAdded(long playerId, List<Item> items, AddType addType) {
        if (itemAddListeners.isEmpty() || items == null || items.isEmpty()) {
            return;
        }
        Map<Integer, Long> added = new HashMap<>();
        for (Item item : items) {
            if (item != null && item.getItemCount() > 0) {
                added.merge(item.getId(), item.getItemCount(), Long::sum);
            }
        }
        if (added.isEmpty()) {
            return;
        }
        Map<Integer, Long> immutable = Collections.unmodifiableMap(added);
        for (ItemAddListener listener : itemAddListeners) {
            try {
                listener.onItemsAdded(playerId, immutable, addType);
            } catch (Exception e) {
                log.error("道具入账监听器异常 listener={},playerId={},items={}",
                        listener.getClass().getSimpleName(), playerId, immutable, e);
            }
        }
    }

    private void notifyItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType) {
        if (itemConsumeListeners.isEmpty() || items == null || items.isEmpty()) {
            return;
        }
        Map<Integer, Long> immutable = Collections.unmodifiableMap(items);
        for (ItemConsumeListener listener : itemConsumeListeners) {
            try {
                listener.onItemsConsumed(playerId, immutable, addType);
            } catch (Exception e) {
                log.error("道具消费监听器异常 listener={},playerId={},items={}",
                        listener.getClass().getSimpleName(), playerId, immutable, e);
            }
        }
    }


    /**
     * 移除道具
     *
     * @param remove 移除的道具
     * @return 最新的背包结果
     */
    public CommonResult<ItemOperationResult> removeItem(Player player, Item remove, AddType addType) {
        if (remove == null) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.PARAM_ERROR);
            result.data = new ItemOperationResult();
            return result;
        }
        return removeItem(player, remove.getId(), remove.getItemCount(), addType);
    }

    /**
     * 移除道具
     */
    public CommonResult<ItemOperationResult> removeItem(Player player, int id, long count, AddType addType) {
        return removeItem(player, null, id, count, addType);
    }

    /**
     * 移除道具
     */
    public CommonResult<ItemOperationResult> removeItem(Player player, Integer girdId, int id, long count,
                                                        AddType addType) {
        return removeItem(player, Collections.singletonList(new Item(girdId, id, count)), addType);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<ItemOperationResult> removeItems(Player player, Map<Integer, Long> removeItemMap,
                                                         AddType addType, String desc) {
        List<Item> itemList = checkItemParam(removeItemMap);
        if (CollectionUtil.isEmpty(itemList)) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
            result.data = new ItemOperationResult();
            return result;
        }
        return removeItem(player, itemList, addType, desc);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<ItemOperationResult> removeItems(Player player, Map<Integer, Long> removeItemMap,
                                                         AddType addType) {
        return removeItems(player, removeItemMap, addType, null);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<ItemOperationResult> removeItem(Player player, List<Item> removeItemList, AddType addType) {
        return removeItem(player, removeItemList, addType, null);
    }

    /**
     * 移除道具
     *
     * @param player 玩家信息
     */
    public CommonResult<ItemOperationResult> removeItem(Player player, List<Item> removeItemList, AddType addType, String desc) {
        if (player == null) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.NOT_FOUND);
            result.data = new ItemOperationResult();
            return result;
        }
        if (removeItemList == null || removeItemList.isEmpty()) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
            result.data = new ItemOperationResult();
            return result;
        }
        List<Item> validRemoveItemList = new ArrayList<>(removeItemList.size());
        for (Item item : removeItemList) {
            if (item == null) {
                continue;
            }
            validRemoveItemList.add(item);
        }
        if (validRemoveItemList.isEmpty()) {
            CommonResult<ItemOperationResult> result = new CommonResult<>(Code.SUCCESS);
            result.data = new ItemOperationResult();
            return result;
        }
        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        player = corePlayerService.get(player.getId());
        int code = checkHasItems(player, validRemoveItemList);
        if (code != Code.SUCCESS) {
            result.code = code;
            return result;
        }
        result.data = new ItemOperationResult();

        long deductGoldV = 0;
        long deductDiamondV = 0;
        long deductShellV = 0;

        long playerId = player.getId();
        //优先扣除特殊道具，剩余的常规道具再走货币与背包；与入账同理，必须在取锁前处理
        List<Item> normalRemoveItemList = new ArrayList<>(validRemoveItemList.size());
        List<Item> removedSpecialItemList = splitSpecialItems(validRemoveItemList, normalRemoveItemList);
        if (!removedSpecialItemList.isEmpty()
                && !specialItemListener.removeItems(playerId, removedSpecialItemList, addType, desc)) {
            log.warn("扣除特殊道具失败 playerId={},items={},addType={}", playerId, removedSpecialItemList, addType);
            result.code = Code.NOT_ENOUGH_ITEM;
            return result;
        }

        String key = getLockKey(playerId);
        boolean lock = false;
        boolean currencyDeducted = false;
        boolean committed = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                result.code = Code.FAIL;
                log.debug("获取锁失败 lockKey:{} playerId = {} ", key, playerId);
                return result;
            }
            List<Item> packItemList = new ArrayList<>();
            for (Item item : normalRemoveItemList) {
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
                //累加扣除贝币
                if (itemCfg.getType() == GameConstant.Item.TYPE_SHELL) {
                    deductShellV += Math.abs(item.getItemCount());
                    continue;
                }
                packItemList.add(item);
            }
            boolean hasMoneyCoin = deductDiamondV > 0 || deductGoldV > 0 || deductShellV > 0;
            // 如果道具列表为空，但是还需要处理金币钻石，继续处理
            if (packItemList.isEmpty() && !hasMoneyCoin) {
                result.code = Code.SUCCESS;
                committed = true;
                return result;
            }

            PlayerPack playerPack = null;
            Map<Integer, Long> changeBefore = new HashMap<>(packItemList.size());
            Map<Integer, Long> consumedMap = new HashMap<>(packItemList.size());
            if (!packItemList.isEmpty()) {
                playerPack = getFromAllDB(playerId);
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
                    changeBefore.putIfAbsent(id, playerPack.getItemCount(id));
                    if (gridId == null) {
                        removeResult = playerPack.removeItem(id, count);
                    } else {
                        removeResult = playerPack.removeItem(gridId, id, count);
                    }
                    if (!removeResult.success()) {
                        result.code = removeResult.code;
                        return result;
                    }
                    consumedMap.merge(id, count, Long::sum);
                }
            }
            //扣除货币
            if (hasMoneyCoin) {
                CommonResult<Player> removeResult =
                        corePlayerService.deductMoneyCoin(playerId, deductGoldV, deductDiamondV, deductShellV, addType, true, desc);
                if (!removeResult.success()) {
                    result.code = removeResult.code;
                    return result;
                }
                currencyDeducted = true;
                result.data.goldChange(-deductGoldV, removeResult.data.getGold());
                result.data.diamondChange(-deductDiamondV, removeResult.data.getDiamond());
                result.data.shellChange(-deductShellV, removeResult.data.getShell());
            }
            if (!packItemList.isEmpty()) {
                redisTemplate.opsForHash().put(tableName, playerId, playerPack);
                //放入最新的道具信息
                Map<Integer, Long> changeAfterNum = new HashMap<>();
                for (Item item : packItemList) {
                    changeAfterNum.put(item.getId(), playerPack.getItemCount(item.getId()));
                }
                result.data.setChangeEndItemNum(changeAfterNum);
                result.data.setChangeBeforeItemNum(changeBefore);
                coreLogger.consumeItem(playerId, changeBefore, consumedMap, changeAfterNum, addType);
            }
            result.code = Code.SUCCESS;
            committed = true;

            // 只有在道具和货币都成功后才触发任务；触发失败不应影响已提交数据的返回码。
            try {
                for (Item item : packItemList) {
                    int id = item.getId();
                    long count = item.getItemCount();
                    taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> {
                        TaskConditionParam12101 param = new TaskConditionParam12101();
                        param.setItemId(id);
                        param.setAddValue(count);
                        return param;
                    });
                }
                //触发消耗金币任务
                if (deductGoldV > 0) {
                    TaskConditionParam12101 param = new TaskConditionParam12101();
                    param.setItemId(ItemUtils.getGoldItemId());
                    param.setAddValue(deductGoldV);
                    param.setResultValue(result.data.getGoldNum());
                    taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> param);
                }
                //触发消耗钻石任务
                if (deductDiamondV > 0) {
                    TaskConditionParam12101 param = new TaskConditionParam12101();
                    param.setItemId(ItemUtils.getDiamondItemId());
                    param.setAddValue(deductDiamondV);
                    param.setResultValue(result.data.getDiamond());
                    taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> param);
                }
                //触发消耗贝币任务
                if (deductShellV > 0) {
                    TaskConditionParam12101 param = new TaskConditionParam12101();
                    param.setItemId(ItemUtils.getShellItemId());
                    param.setAddValue(deductShellV);
                    param.setResultValue(result.data.getShell());
                    taskManager.trigger(playerId, TaskConstant.ConditionType.PLAY_USE_ITEM, () -> param);
                }
            } catch (Exception e) {
                log.error("移除道具成功后触发任务失败 playerId={}", playerId, e);
            }
            //货币同样算消费(主线 12220 就是按金币/钻石 itemId 过滤的)，分拣时货币未进 consumedMap；
            //另建一份，避免污染已交给 coreLogger 的那个 map
            Map<Integer, Long> consumedWithCurrency = new HashMap<>(consumedMap);
            if (deductGoldV > 0) {
                consumedWithCurrency.merge(ItemUtils.getGoldItemId(), deductGoldV, Long::sum);
            }
            if (deductDiamondV > 0) {
                consumedWithCurrency.merge(ItemUtils.getDiamondItemId(), deductDiamondV, Long::sum);
            }
            if (deductShellV > 0) {
                consumedWithCurrency.merge(ItemUtils.getShellItemId(), deductShellV, Long::sum);
            }
            notifyItemsConsumed(playerId, consumedWithCurrency, addType);
            return result;
        } catch (Exception e) {
            if (currencyDeducted && !committed) {
                rollbackDeductedCurrency(playerId, deductGoldV, deductDiamondV, deductShellV);
            }
            result.code = Code.FAIL;
            log.error("移除道具，保存 playerPack 失败 playerId={}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }
            //背包或货币未提交成功，已扣除的特殊道具要还回去
            if (!committed) {
                rollbackRemovedSpecialItems(playerId, removedSpecialItemList);
            }
        }
        return result;
    }

    private void rollbackAddedCurrency(long playerId, long goldNum, long diamondNum, long shellNum) {
        if (goldNum <= 0 && diamondNum <= 0 && shellNum <= 0) {
            return;
        }
        CommonResult<Player> rollbackResult =
                corePlayerService.deductMoneyCoin(playerId, goldNum, diamondNum, shellNum, AddType.FAIL_ROLLBACK, false, "PlayerPackService addItems rollback");
        if (!rollbackResult.success()) {
            log.error("添加道具回滚货币失败(需人工修复) playerId={},gold={},diamond={},shellNum={},code={}", playerId, goldNum, diamondNum, shellNum, rollbackResult.code);
        }
    }

    private void rollbackDeductedCurrency(long playerId, long goldNum, long diamondNum, long shellNum) {
        if (goldNum <= 0 && diamondNum <= 0 && shellNum <= 0) {
            return;
        }
        CommonResult<Player> rollbackResult =
                corePlayerService.addMoneyCoin(playerId, goldNum, diamondNum, shellNum, AddType.FAIL_ROLLBACK, false, "PlayerPackService removeItem rollback");
        if (!rollbackResult.success()) {
            log.error("移除道具回滚货币失败(需人工修复) playerId={},gold={},diamond={},shellNum={},code={}", playerId, goldNum, diamondNum, shellNum, rollbackResult.code);
        }
    }

    /**
     * 检查是否拥有道具
     *
     * @param player  玩家
     * @param itemMap 拥有道具
     * @return true 拥有 false 未拥有
     */
    public boolean checkHasItems(Player player, Map<Integer, Long> itemMap) {
        if (player == null) {
            return false;
        }
        if (itemMap == null || itemMap.isEmpty()) {
            return true;
        }
        long playerId = player.getId();

        try {
            List<Item> itemList = checkItemParam(itemMap);
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
        if (player == null) {
            return Code.NOT_FOUND;
        }
        if (itemList == null || itemList.isEmpty()) {
            return Code.SUCCESS;
        }
        long playerId = player.getId();
        try {
            PlayerPack playerPack = getFromAllDB(playerId);
            for (Item item : itemList) {
                if (item == null) {
                    continue;
                }
                //特殊道具由承载它的处理器校验，不查背包与货币
                if (isSpecialItem(item.getId())) {
                    if (specialItemListener.getItemCount(playerId, item.getId()) < item.getItemCount()) {
                        return Code.NOT_ENOUGH;
                    }
                    continue;
                }
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
                if (itemCfg.getType() == GameConstant.Item.TYPE_SHELL) {
                    if (player.getShell() < item.getItemCount()) {
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
     * @param useItemId
     * @return
     */
    public CommonResult<ItemOperationResult> useItem(Player player, int useItemId, long useItemCount,
                                                     Map<Integer, Long> addItemsMap,
                                                     AddType addType) {
        return useItem(player, null, useItemId, useItemCount, addItemsMap, addType);
    }


    /**
     * 使用道具
     *
     * @param useItemId
     * @return
     */
    public CommonResult<ItemOperationResult> useItem(Player player, Integer girdId, int useItemId, long useItemCount,
                                                     Map<Integer, Long> addItemsMap,
                                                     AddType addType) {
        CommonResult<ItemOperationResult> result = new CommonResult<>(Code.FAIL);

        CommonResult<ItemOperationResult> removeResult = removeItem(player, girdId, useItemId, useItemCount, addType);
        if (!removeResult.success()) {
            result.code = removeResult.code;
            return result;
        }
        CommonResult<ItemOperationResult> addResult = addItems(player.getId(), addItemsMap, addType);
        if (!addResult.success()) {
            //添加失败，要将之前扣除的道具加回去
            CommonResult<ItemOperationResult> rollbackResult = addItem(player.getId(), useItemId, useItemCount, AddType.FAIL_ROLLBACK);
            if (!rollbackResult.success()) {
                log.error("使用道具失败后回滚道具失败(需人工修复) playerId={},girdId={},useItemId={},useItemCount={},addCode={},rollbackCode={}",
                        player.getId(), girdId, useItemId, useItemCount, addResult.code, rollbackResult.code);
                result.code = Code.FAIL;
                return result;
            }
            result.code = addResult.code;
            log.debug("使用道具时，添加失败 playerId = {},girdId = {},useItemId = {}", player.getId(), girdId, useItemId);
            return result;
        }
        result.code = Code.SUCCESS;
        result.data = addResult.data;
        return result;
    }

    public PlayerPack checkAndSave(long playerId, DataSaveCallback<PlayerPack> cbk) {
        String key = getLockKey(playerId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.debug("获取锁失败 lockKey:{} playerId = {} ", key, playerId);
                return null;
            }
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
            if (lock) {
                redisLock.tryUnlock(key);
            }
        }
        return null;
    }

    public PlayerPack doSave(long playerId, DataSaveCallback<PlayerPack> cbk) {
        String key = getLockKey(playerId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.debug("获取锁失败 lockKey:{} playerId = {} ", key, playerId);
                return null;
            }
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
            if (lock) {
                redisLock.tryUnlock(key);
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
     * 读取道具当前持有量：特殊道具走承载它的处理器，其余读背包（不含金币等货币）
     *
     * @param playerId 玩家ID
     * @param itemId   道具ID
     * @return 持有量
     */
    public long getItemCount(long playerId, int itemId) {
        if (isSpecialItem(itemId)) {
            return specialItemListener.getItemCount(playerId, itemId);
        }
        PlayerPack playerPack = getFromAllDB(playerId);
        return playerPack == null ? 0 : playerPack.getItemCount(itemId);
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

    /**
     * 获取玩家的背包数据
     *
     * @param playerId
     * @return
     */
    public List<PackItemInfo> getPlayerPack(long playerId) {
        PlayerPack playerPack = getFromAllDB(playerId);
        if (playerPack == null || playerPack.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        List<PackItemInfo> packItemInfos = new ArrayList<>();
        playerPack.getItems().forEach((key, value) -> {
            ItemCfg itemCfg = GameDataManager.getItemCfg(value.getId());
            if (itemCfg != null) {
                PackItemInfo info = new PackItemInfo();
                info.girdId = key;
                info.item = new ItemInfo();
                info.item.itemId = value.getId();
                info.item.count = value.getItemCount();
                packItemInfos.add(info);
            }
        });
        return packItemInfos;
    }
}
