package com.jjg.game.core.listener;

import com.jjg.game.common.baselogic.IGameSysFuncInterface;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2025/10/30 13:56
 */
public interface OrderGenerate extends IGameSysFuncInterface {
    /**
     * 获取对应渠道配置的商品品ID,以及配置价格
     *
     * @param player 玩家id
     * @param req    请求
     * @return 商品id,配置价格
     */
    BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req);

    /**
     * 获取对应充值类型
     *
     * @return 充值类型
     */
    RechargeType getRechargeType();


    /**
     * 收到充值的处理
     *
     * @param player 玩家数据
     * @param order  订单数据
     * @return `true` 表示处理成功，`false` 表示处理失败
     */
    boolean onReceivedRecharge(Player player, Order order);

    /**
     * 执行本身逻辑后是否还需要继续执行
     *
     * @return
     */
    default boolean isContinue(Order order) {
        return true;
    }
}
