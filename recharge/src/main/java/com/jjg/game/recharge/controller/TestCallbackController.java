package com.jjg.game.recharge.controller;

import com.jjg.game.core.data.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author 11
 * @date 2025/9/22 20:17
 */
@RestController
@RequestMapping(method = {RequestMethod.GET}, value = "testpay")
public class TestCallbackController extends AbstractCallbackController {
    /**
     * 回调
     *
     * @return
     */
    @RequestMapping("callback")
    public boolean callback(@RequestParam Map<String, String> map) {
        try {
            log.info("收到测试充值回调 map = {}", map);
            String orderId = map.get("orderId");
            Order order = orderService.getOrder(orderId);
            if (order == null) {
                log.debug("未找到该订单 orderId = {}", orderId);
                return false;
            }
            //处理充值回调逻辑
            payCallback(order);
            return true;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }
}
