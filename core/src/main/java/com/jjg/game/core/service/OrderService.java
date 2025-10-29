package com.jjg.game.core.service;

import cn.hutool.core.date.DateUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.OrderDao;
import com.jjg.game.core.data.*;
import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author 11
 * @date 2025/9/18 15:03
 */
@Service
public class OrderService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private AccountDao accountDao;

    public Order generateOrder(Player player, PayType payType, String productId, RechargeType rechargeType) {
        Account account = accountDao.queryAccountByPlayerId(player.getId());
        return generateOrder(player.getId(), account.getChannel(), payType, productId, BigDecimal.ZERO, rechargeType);
    }

    public Order generateOrder(Player player, PayType payType, String productId, BigDecimal price, RechargeType rechargeType) {
        Account account = accountDao.queryAccountByPlayerId(player.getId());
        return generateOrder(player.getId(), account.getChannel(), payType, productId, price, rechargeType);
    }

    /**
     * 生成订单
     *
     * @param playerId
     * @param productId
     * @param price
     * @return
     */
    public Order generateOrder(long playerId, ChannelType playerChannel, PayType payType, String productId, BigDecimal price, RechargeType rechargeType) {
        return generateOrder(playerId, playerChannel, payType, productId, price, rechargeType, OrderStatus.ORDER, null);
    }

    /**
     * 生成订单
     *
     * @param playerId
     * @param productId
     * @param price
     * @return
     */
    public Order generateOrder(long playerId, ChannelType playerChannel, PayType payType, String productId, BigDecimal price, RechargeType rechargeType, OrderStatus orderStatus, String channelProductId) {
        for (int i = 0; i < CoreConst.Common.MONGO_TRY_COUNT; i++) {
            String orderId = "cz" + DateUtil.format(DateUtil.date(), "yyMMdd") + RandomUtils.getRandomString(9);
            String uuid = RandomUtils.getOriginalUUid().toUpperCase();
            try {
                Order order = new Order();
                order.setId(orderId);
                order.setUuid(uuid);
                order.setPlayerId(playerId);
                order.setPlayerChannel(playerChannel.getValue());
                order.setPayChannel(payType.getValue());
                order.setProductId(productId);
                order.setPrice(price);

                order.setOrderStatus(orderStatus);
                order.setRechargeType(rechargeType);
                order.setCreateTime(TimeHelper.nowInt());
                order.setChannelOrderId(channelProductId);
                orderDao.insert(order);
                return order;
            } catch (DuplicateKeyException e) {
                log.info("订单id重复，进行重试 orderId = {},i = {}", orderId, i);
            }
        }
        return null;
    }

    public Order orderSuccess(String orderId, String channelOrderId) {
        return orderDao.changeOrderSuccess(orderId, channelOrderId);
    }

    public Order orderFail(String orderId) {
        return orderDao.changeOrderFail(orderId);
    }

    public Order getOrder(String orderId) {
        return orderDao.getOrderById(orderId);
    }

    public Order getOrderByUUid(String uuid) {
        return orderDao.getOrderByUUid(uuid);
    }
}
