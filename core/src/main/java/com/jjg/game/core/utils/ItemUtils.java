package com.jjg.game.core.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 道具工具类
 *
 * @author 2CL
 */
public class ItemUtils {

    /**
     * 获取金币道具ID
     */
    public static int getGoldItemId() {
        return GameDataManager.getItemCfgList().stream().filter(
                itemCfg -> itemCfg.getType() == GameConstant.Item.TYPE_GOLD
        ).map(ItemCfg::getId).findFirst().orElse(0);
    }

    /**
     * 获取钻石道具ID
     */
    public static int getDiamondItemId() {
        return GameDataManager.getItemCfgList().stream().filter(
                itemCfg -> itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND
        ).map(ItemCfg::getId).findFirst().orElse(0);
    }

    public static List<ItemInfo> buildItemInfo(Map<Integer, Long> itemInfo) {
        List<ItemInfo> itemInfos = new ArrayList<>();
        for (Map.Entry<Integer, Long> longEntry : itemInfo.entrySet()) {
            ItemInfo info = new ItemInfo();
            info.itemId = longEntry.getKey();
            info.count = longEntry.getValue();
            itemInfos.add(info);
        }
        return itemInfos;
    }

    public static List<Item> buildItems(Map<Integer, Long> itemInfo) {
        List<Item> items = new ArrayList<>();
        for (Map.Entry<Integer, Long> longEntry : itemInfo.entrySet()) {
            Item item = new Item();
            item.setId(longEntry.getKey());
            item.setItemCount(longEntry.getValue());
            items.add(item);
        }
        return items;
    }

    public static List<Item> buildItemsByStrList(List<String> itemList) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < itemList.size(); i += 2) {
            Item item = new Item();
            int id = Integer.parseInt(itemList.get(i));
            long count = Long.parseLong(itemList.get(i + 1));
            item.setId(id);
            item.setItemCount(count);
            items.add(item);
        }
        return items;
    }


    public static ItemInfo buildItemInfo(int itemId, long count) {
        ItemInfo info = new ItemInfo();
        info.count = count;
        info.itemId = itemId;
        return info;
    }

    public static ItemInfo buildGoldInfo(long num) {
        ItemInfo info = new ItemInfo();
        info.itemId = getGoldItemId();
        info.count = num;
        return info;
    }

    public static long getGoldNum(Map<Integer, Long> itemInfo) {
        int goldItemId = getGoldItemId();
        return itemInfo.getOrDefault(goldItemId, 0L);
    }

    /**
     * 构建道具列表
     *
     * @param itemInfo 输入的道具信息列表，按顺序包含道具ID和对应数量，每两个整数为一个完整的道具信息。
     *                 若列表为空或长度不是偶数，将返回空的道具列表。
     * @return 返回构建的道具信息列表，每个道具信息包含道具ID和对应数量。
     */
    public static List<ItemInfo> buildItemInfos(List<Integer> itemInfo) {
        List<ItemInfo> items = new ArrayList<>();
        if (itemInfo == null || itemInfo.isEmpty()) {
            return items;
        }
        if (itemInfo.size() % 2 != 0) {
            return items;
        }
        for (int i = 0; i < itemInfo.size(); i += 2) {
            ItemInfo item = new ItemInfo();
            item.itemId = itemInfo.get(i);
            item.count = itemInfo.get(i + 1);
            items.add(item);
        }
        return items;
    }

    /**
     * 根据输入的道具信息列表构建道具列表。
     * <p>
     * 道具信息列表中每两个整数为一组，分别表示道具ID和对应的数量。
     * 如果输入的列表为空或长度不是偶数，则返回空的道具列表。
     *
     * @param itemInfo 输入的道具信息列表，按顺序包含道具ID和对应的数量。
     * @return 构建的道具列表，其中每个道具包含道具ID和对应数量。
     */
    public static List<Item> buildItems(List<Integer> itemInfo) {
        List<Item> items = new ArrayList<>();
        if (itemInfo == null || itemInfo.isEmpty()) {
            return items;
        }
        if (itemInfo.size() % 2 != 0) {
            return items;
        }
        for (int i = 0; i < itemInfo.size(); i += 2) {
            Item item = new Item();
            item.setId(itemInfo.get(i));
            item.setItemCount(itemInfo.get(i + 1));
            items.add(item);
        }
        return items;
    }

    /**
     * 合并道具
     * @param mergeMap1 需要合并的道具1
     * @param mergeMap2 需要合并的道具2
     * @return 合并后的道具
     */
    public static Map<Integer, Long> mergeItems(Map<Integer, Long> mergeMap1, Map<Integer, Long> mergeMap2) {
        if (CollectionUtil.isEmpty(mergeMap1) || CollectionUtil.isEmpty(mergeMap2)) {
            return Map.of();
        }
        Map<Integer, Long> tempMap = new HashMap<>();
        mergeMap1.forEach((key, value) -> tempMap.merge(key, value, Long::sum));
        mergeMap2.forEach((key, value) -> tempMap.merge(key, value, Long::sum));
        return tempMap;
    }

    /**
     * 判断道具是不是都是货币
     * @param itemsMap 道具列表
     * @return 货币id
     */
    public static int isAllCurrencyItems(Map<Integer, Long> itemsMap) {
        if (itemsMap == null) {
            return 0;
        }
        if (itemsMap.size() != 1) {
            return 0;
        }
        for (Integer itemId : itemsMap.keySet()) {
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null || (itemCfg.getType() != GameConstant.Item.TYPE_GOLD
                    && itemCfg.getType() != GameConstant.Item.TYPE_DIAMOND)) {
                return 0;
            }
        }
        return itemsMap.keySet().iterator().next();
    }
}
