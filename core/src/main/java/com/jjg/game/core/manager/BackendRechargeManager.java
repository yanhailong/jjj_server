package com.jjg.game.core.manager;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.service.PlayerPackService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BackendRechargeManager implements OrderGenerate {
    private static final Logger log = LoggerFactory.getLogger(BackendRechargeManager.class);

    @Autowired
    private PlayerPackService playerPackService;

    /**
     * 处理后台充值任务
     *
     * @param player
     * @param order
     */
    protected boolean dealBackendRecharge(Player player, Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            log.error("后台充值奖励为空 playerId:{} orderId:{}", player.getId(), order.getId());
            return false;
        }
        CommonResult<ItemOperationResult> addItemsResult =
                playerPackService.addItems(player.getId(), order.getItems(), AddType.BACKEND_OPERATOR, order.getId());
        if (!addItemsResult.success()) {
            log.error("后台充值发奖失败 playerId:{} orderId:{} code:{}",
                    player.getId(), order.getId(), addItemsResult.code);
            return false;
        }
        return true;
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        return BigDecimal.ZERO;
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.BACKEND;
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType()) {
            return true;
        }
        return dealBackendRecharge(player, order);
    }

    @Override
    public boolean isContinue(Order order) {
        return StringUtils.isBlank(order.getDesc());
    }
}
