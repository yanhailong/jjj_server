package com.jjg.game.core.listener;

import com.jjg.game.core.constant.AddType;

import java.util.Map;

/** 道具成功扣除监听器（仅背包道具，不含货币与特殊道具）。 */
public interface ItemConsumeListener {
    void onItemsConsumed(long playerId, Map<Integer, Long> items, AddType addType);
}
