package com.jjg.game.core.listener;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Item;

import java.util.List;

/**
 * 特殊道具处理接口
 * <p>
 * 由业务模块实现，接管不由背包与货币承载的道具（如 sim 的能量、知名度、赛季币、勋章）。
 * {@link #support(int)} 命中的道具由本接口独立入账与扣除，不再进入背包与货币流程。
 */
public interface SpecialItemListener {

    /**
     * 该道具是否由本处理器承载
     *
     * @param itemId 道具id
     * @return true 由本处理器承载，不入背包与货币
     */
    boolean support(int itemId);

    /**
     * 特殊道具当前持有量
     *
     * @param playerId 玩家id
     * @param itemId   道具id
     * @return 持有量
     */
    long getItemCount(long playerId, int itemId);

    /**
     * 特殊道具入账
     *
     * @param items 均为 {@link #support(int)} 命中的道具
     * @return true 入账成功
     */
    boolean addItems(long playerId, List<Item> items, AddType addType, String desc, boolean notify);

    /**
     * 特殊道具扣除，先校验充足再扣除，任一不足整体失败且不产生扣除
     *
     * @param items 均为 {@link #support(int)} 命中的道具
     * @return true 扣除成功
     */
    boolean removeItems(long playerId, List<Item> items, AddType addType, String desc);
}
