package com.jjg.game.recharge.controller;

import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lm
 * @date 2025/11/3 14:36
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "test")
public class RechargeTestController extends AbstractCallbackController {
    private  final Logger log = LoggerFactory.getLogger(RechargeTestController.class);

    /**
     * gm通过订单调用充值
     */
    //
    @RequestMapping("rechargeByOrder/{orderId}")
    public ResponseEntity<String> rechargeByOrder(@PathVariable("orderId") String orderId) {
        try {
            log.info("收到后台的请求订单充值 id = {}", orderId);
            Order order = orderService.getOrder(orderId);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            order.setChannelOrderId("test");
            if (!checkOrder(order)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            Player player = playerService.get(order.getPlayerId());
            //获取玩家session信息
            PlayerSessionInfo info = playerSessionService.getInfo(order.getPlayerId());
            String money = order.getPrice().toPlainString();
            String regionCode = "test";
            coreLogger.order(player, order, money, regionCode);
            log.info("玩家充值成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            //将充值成功消息通知玩家所在节点
            notifyPlayerCurrentNode(info, order, money, regionCode);
            //返回修改结果
            return ResponseEntity.ok("common.success");
        } catch (Exception e) {
            log.error("", e);
            return ResponseEntity.ok("common.exception");
        }
    }
}
