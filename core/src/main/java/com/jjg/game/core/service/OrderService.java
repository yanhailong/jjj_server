package com.jjg.game.core.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.OrderDao;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.OrderStatus;
import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 11
 * @date 2025/9/18 15:03
 */
@Service
public class OrderService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderDao orderDao;

    /**
     * 生成订单
     *
     * @param playerId
     * @param productId
     * @param price
     * @return
     */
    public Order generateOrder(long playerId, int productId, long price) {
        for (int i = 0; i < CoreConst.Common.MONGO_TRY_COUNT; i++) {
            String orderId = DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS") + RandomUtil.randomNumbers(4);
            try {
                Order order = new Order();
                order.setId(orderId);
                order.setPlayerId(playerId);
                order.setProductId(productId);
                order.setPrice(price);

                order.setOrderStatus(OrderStatus.ORDER);
                order.setCreateTime(TimeHelper.nowInt());
                orderDao.insert(order);
                return order;
            } catch (DuplicateKeyException e) {
                log.info("订单id重复，进行重试 orderId = {},i = {}", orderId, i);
            }
        }
        return null;
    }

    public Order orderSuccess(String orderId) {
        return orderDao.changeOrderSuccess(orderId);
    }
}
