package com.jjg.game.core.utils;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;

import java.util.ArrayList;
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

}
