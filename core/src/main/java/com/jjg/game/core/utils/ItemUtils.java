package com.jjg.game.core.utils;

import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;

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
}
