package com.jjg.game.core.listener;

import com.jjg.game.core.constant.AddType;

import java.util.Map;

/** 道具成功入账监听器。 */
public interface ItemAddListener {
    void onItemsAdded(long playerId, Map<Integer, Long> items, AddType addType);
}
