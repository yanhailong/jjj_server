package com.jjg.game.core.base.item;

import com.jjg.game.core.data.Item;
import com.jjg.game.sampledata.bean.ItemCfg;

/**
 * 道具使用接口
 *
 * @author 2CL
 */
public interface IItemUseInterface {

    /**
     * 自动使用
     * @param playerId 玩家id
     * @param item 道具数据
     * @param itemCfg 道具配置
     * @return 自动使用的数量
     */
    int autoUse(long playerId, Item item, ItemCfg itemCfg);
}
