package com.jjg.game.core.base.item;

import com.jjg.game.core.data.Item;
import com.jjg.game.sampledata.bean.ItemCfg;

import java.util.List;

/**
 * 道具使用接口
 *
 * @author 2CL
 */
public interface IItemUseInterface {

    /**
     * 自动使用
     */
    void autoUse(long playerId, List<Item> itemList);
}
