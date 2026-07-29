package com.jjg.game.core.listener;

import com.jjg.game.core.constant.AddType;

import java.util.Set;

/**
 * 道具扣除不足监听器。
 * itemIds 只包含本次实际不够扣的道具ID，不要求玩家剩余数量为0。
 */
public interface ItemNotEnoughListener {

    void onItemsNotEnough(long playerId, Set<Integer> itemIds, AddType addType);
}
