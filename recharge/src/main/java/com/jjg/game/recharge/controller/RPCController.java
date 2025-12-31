package com.jjg.game.recharge.controller;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.rpc.BackendBridge;
import org.springframework.stereotype.Component;

@Component
public class RPCController extends AbstractCallbackController implements BackendBridge {

    @Override
    public int recharge(String selfOrderId, String channelOrderId, long price, int rechargeType) {
        try {
            log.info("收到后台充值的请求 selfOrderId = {},channelOrderId = {},price = {},rechargeType = {}",
                    selfOrderId, channelOrderId, price, rechargeType);

            Order order = orderService.getOrder(selfOrderId);
            if (order == null) {
                log.warn("未找到该订单 selfOrderId = {}", selfOrderId);
                return Code.NOT_FOUND;
            }
            order.setChannelOrderId(channelOrderId);
            order = checkOrder(order);
            if (order == null) {
                return Code.FAIL;
            }
            Player player = playerService.get(order.getPlayerId());
            //获取玩家session信息
            PlayerSessionInfo info = playerSessionService.getInfo(order.getPlayerId());
            String money = order.getPrice().toPlainString();
            String regionCode = "backend";
            coreLogger.order(player, order, money, regionCode, order.getProductId());
            log.info("玩家通过后台充值成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //将充值成功消息通知玩家所在节点
            notifyPlayerCurrentNode(info, order, money, regionCode, order.getChannelProductId());
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }
}
