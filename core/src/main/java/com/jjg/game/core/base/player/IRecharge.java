package com.jjg.game.core.base.player;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.ShopProduct;

/**
 * @author 11
 * @date 2025/9/22 20:11
 */
public interface IRecharge extends IGameSysFuncInterface {

    /**
     * 充值成功
     *
     * @param player
     * @param order 订单
     * @param product 商品
     */
    void rechargeSuccess(Player player, Order order, ShopProduct product);
}
