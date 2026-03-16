package com.jjg.game.core.listener;

import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;

/**
 * @author 11
 * @date 2026/3/16
 */
public interface TmpRechargeListener {
    void recharge(Player player, Order order);
}
