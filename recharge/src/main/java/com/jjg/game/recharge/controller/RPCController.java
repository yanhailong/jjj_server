package com.jjg.game.recharge.controller;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.rpc.BackendBridge;
import org.springframework.stereotype.Component;

@Component
public class RPCController extends AbstractCallbackController implements BackendBridge {

    @Override
    public int recharge(String selfOrderId, String channelOrderId) {
        try {
            log.info("收到后台充值的请求 selfOrderId = {},channelOrderId = {}",
                    selfOrderId, channelOrderId);

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
            //调用充值
            payCallback(order, order.getPrice().toPlainString(), "backend","backend");
            return Code.SUCCESS;
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }
}
