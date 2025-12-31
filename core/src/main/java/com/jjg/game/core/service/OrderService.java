package com.jjg.game.core.service;

import cn.hutool.core.date.DateUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.OrderDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.RechargeType;
import com.mongodb.DuplicateKeyException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        return generateOrder(playerId, playerChannel, payType, productId, price, rechargeType, OrderStatus.ORDER, null, null);
    }

    private Order generateOrder(long playerId, ChannelType playerChannel, PayType payType, String productId, BigDecimal price,
                                RechargeType rechargeType, OrderStatus orderStatus, String channelProductId, List<Item> items) {
        return generateOrder(null, playerId, playerChannel, payType, productId, price, rechargeType, orderStatus, channelProductId, items);
    }

    public Order generateOrder(String orderIdPrefix, long playerId, BigDecimal price, RechargeType rechargeType, List<Item> items) {
        Account account = accountDao.queryAccountByPlayerId(playerId);
        return generateOrder(orderIdPrefix, playerId, account.getChannel(), null, null, price, rechargeType, OrderStatus.ORDER, null, items);
    }

    /**
     * 生成订单
     *
     * @param playerId
     * @param productId
     * @param price
     * @return
     */
    private Order generateOrder(String orderIdPrefix, long playerId, ChannelType playerChannel, PayType payType, String productId, BigDecimal price,
                                RechargeType rechargeType, OrderStatus orderStatus, String channelProductId, List<Item> items) {
        for (int i = 0; i < CoreConst.Common.MONGO_TRY_COUNT; i++) {
            String orderId = (StringUtils.isBlank(orderIdPrefix) ? "cz" : orderIdPrefix) + DateUtil.format(DateUtil.date(), "yyMMdd") + RandomUtils.getRandomString(9);
            String uuid = RandomUtils.getOriginalUUid();
            try {
                Order order = new Order();
                order.setId(orderId);
                order.setUuid(uuid);
                order.setPlayerId(playerId);
                if(playerChannel != null){
                    order.setPlayerChannel(playerChannel.getValue());
                }
                if(payType != null){
                    order.setPayChannel(payType.getValue());
                }
                order.setProductId(productId);
                order.setPrice(price);

                order.setOrderStatus(orderStatus);
                order.setRechargeType(rechargeType);
                order.setCreateTime(TimeHelper.nowInt());
                order.setChannelProductId(channelProductId);
                order.setItems(items);
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

    /**
     * 清除创建时间早于指定时间戳的订单
     *
     * @return 删除的订单数量
     */
    public void clean() {
        int expire = TimeHelper.nowInt() - (int) TimeUnit.DAYS.toSeconds(60);
        long delCount = orderDao.deleteOrdersBeforeTimestamp(expire);
        log.info("删除过期订单数量 = {}", delCount);
    }
}
