package com.jjg.game.recharge.controller;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.ShopProduct;
import com.jjg.game.core.data.WebResult;
import org.apache.commons.lang3.StringUtils;
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
    public WebResult<String> callback(@RequestParam Map<String, String> map) {
        try {
            log.info("收到测试充值回调 map = {}", map);
            String orderId = map.get("orderId");
            Order order = orderService.getOrder(orderId);
            if (order == null) {
                log.debug("未找到该订单 orderId = {}", orderId);
                return new WebResult<>(Code.NOT_FOUND, "未找到该订单 orderId = " + orderId);
            }

            boolean checkOrder = true;
            String checkOrderStr = map.get("checkOrder");
            if (StringUtils.isNotEmpty(checkOrderStr)) {
                checkOrder = Boolean.parseBoolean(checkOrderStr);
            }

            ShopProduct shopProduct;
            if (checkOrder) {
                CommonResult<ShopProduct> checkOrderResult = checkOrder(order);
                if (!checkOrderResult.success()) {
                    return failByCode(order, checkOrderResult.code);
                }
                shopProduct = checkOrderResult.data;
            } else {
                shopProduct = shopService.getShopProduct(order.getProductId());
            }

            if (shopProduct == null) {
                log.debug("根据订单未找到商品 orderId = {},productId = {}", orderId, order.getProductId());
                return new WebResult<>(Code.NOT_FOUND, "根据订单未找到商品 orderId = " + orderId + ",productId = " + order.getProductId());
            }
            //处理充值回调逻辑
            payCallback(order, shopProduct);
            return new WebResult<>(Code.SUCCESS);
        } catch (Exception e) {
            log.error("", e);
            return new WebResult<>(Code.EXCEPTION);
        }
    }

    private WebResult<String> failByCode(Order order, int code) {
        if (code == Code.REPEAT_OP) {
            return new WebResult<>(Code.NOT_FOUND, "该订单重复回调 orderId = " + order.getId());
        }

        if (code == Code.PARAM_ERROR) {
            new WebResult<>(Code.NOT_FOUND, "未找到该订单 orderId = " + order.getId());
        }

        if (code == Code.PARAM_ERROR) {
            return new WebResult<>(Code.NOT_FOUND, "根据订单未找到商品 orderId = " + order.getId() + ",productId = " + order.getProductId());
        }
        return new WebResult<>(Code.FAIL);
    }
}
