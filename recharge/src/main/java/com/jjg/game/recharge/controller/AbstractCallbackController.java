package com.jjg.game.recharge.controller;

import com.jjg.game.common.baselogic.function.SystemInterfaceHolder;
import com.jjg.game.core.base.player.IRecharge;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 11
 * @date 2025/9/22 19:37
 */
public abstract class AbstractCallbackController {
    protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected OrderService orderService;
    @Autowired
    protected ShopService shopService;
    @Autowired
    protected PlayerPackService playerPackService;
    @Autowired
    protected CorePlayerService playerService;

    /**
     * 回调获取订单后处理逻辑
     *
     * @param order
     * @return
     */
    protected void payCallback(Order order) {
        if (order.getOrderStatus() == OrderStatus.SUCCESS) {
            log.debug("该订单重复回调 orderId = {}", order.getId());
            return;
        }

        //修改订单状态
        Order successOrder = orderService.orderSuccess(order.getId());
        if (successOrder == null) {
            log.warn("未找到该清单 orderId = {},status = {}", order.getId(), OrderStatus.ORDER);
            //TODO 记录下来，检查该订单，这里不能再次修改订单状态，因为可能是多线程问题没有修改成功
            return;
        }

        //获取商品
        ShopProduct shopProduct = shopService.getShopProduct(order.getProductId());
        if (shopProduct == null) {
            //TODO 记录下来，检查该订单
            return;
        }

        if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
            CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(order.getPlayerId(), shopProduct.getRewardItems(), "recharge", order.getId());
            if(!addItemsResult.success()){
                log.warn("支付成功，但是添加道具失败 playerId = {},orderId = {},productId = {},code = {}", order.getPlayerId(), order.getId(),shopProduct.getId(),addItemsResult.code);
            }
        }

        Player player = playerService.get(order.getPlayerId());

        log.info("玩家充值成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
    }
}
