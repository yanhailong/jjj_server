package com.jjg.game.core.listener;

import com.jjg.game.core.data.Player;

/**
 * @author 11
 * @date 2026/7/1
 */
public interface ItemListener {
    /**
     * 使用道具监听接口
     *
     * @param player
     * @param itemId
     * @param useItemCount
     * @param selectItemId
     */
    void useItem(Player player, int itemId, long useItemCount, int selectItemId, long finalSelectItemCount);
}
