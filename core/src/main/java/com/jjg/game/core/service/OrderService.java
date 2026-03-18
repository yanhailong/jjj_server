package com.jjg.game.core.service;

import cn.hutool.core.date.DateUtil;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.OrderDao;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.RechargeType;
import com.mongodb.DuplicateKeyException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private PlayerRechargeFlowDao playerRechargeFlowDao;

    private final String CHANNEL_ORDER_TABLE_NAME = "channelOrder:data";
    private final String CHANNEL_ORDER_TIME_TABLE_NAME = "channelOrder:time";

    public Order generateOrder(Player player, PayType payType, String productId, RechargeType rechargeType, String desc) {
        Account account = accountDao.queryAccountByPlayerId(player.getId());
        return generateOrder(player.getId(), account.getChannel(), payType, productId, BigDecimal.ZERO, rechargeType, null);
    }

    public Order generateOrder(Player player, PayType payType, String productId, BigDecimal price, RechargeType rechargeType, String desc) {
        Account account = accountDao.queryAccountByPlayerId(player.getId());
        return generateOrder(player.getId(), account.getChannel(), payType, productId, price, rechargeType, desc);
    }

    /**
     * 生成订单
     *
     * @param playerId
     * @param productId
     * @param price
     * @return
     */
    public Order generateOrder(long playerId, ChannelType playerChannel, PayType payType, String productId, BigDecimal price, RechargeType rechargeType, String desc) {
        return generateOrder(playerId, playerChannel, payType, productId, price, rechargeType, OrderStatus.ORDER, productId, null, desc);
    }

    private Order generateOrder(long playerId, ChannelType playerChannel, PayType payType, String productId, BigDecimal price,
                                RechargeType rechargeType, OrderStatus orderStatus, String channelProductId, List<Item> items, String desc) {
        return generateOrder(null, playerId, playerChannel, payType, productId, price, rechargeType, orderStatus, channelProductId, items, desc);
    }

    public Order generateOrder(String orderIdPrefix, long playerId, BigDecimal price, RechargeType rechargeType, List<Item> items, String desc) {
        Account account = accountDao.queryAccountByPlayerId(playerId);
        PayType payType = null;
        if (account.getChannel() != null) {
            payType = PayType.valueOf(account.getChannel().getValue());
        }
        return generateOrder(orderIdPrefix, playerId, account.getChannel(), payType, null, price, rechargeType, OrderStatus.ORDER, null, items, desc);
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
                                RechargeType rechargeType, OrderStatus orderStatus, String channelProductId, List<Item> items, String desc) {
        for (int i = 0; i < CoreConst.Common.MONGO_TRY_COUNT; i++) {
            String orderId = (StringUtils.isBlank(orderIdPrefix) ? "cz" : orderIdPrefix) + DateUtil.format(DateUtil.date(), "yyMMdd") + RandomUtils.getRandomString(9);
            String uuid = RandomUtils.getOriginalUUid();
            try {
                Order order = new Order();
                order.setId(orderId);
                order.setUuid(uuid);
                order.setPlayerId(playerId);
                if (playerChannel != null) {
                    order.setPlayerChannel(playerChannel.getValue());
                }
                if (payType != null) {
                    order.setPayChannel(payType.getValue());
                }
                order.setProductId(productId);
                order.setPrice(price);

                order.setOrderStatus(orderStatus);
                order.setRechargeType(rechargeType);
                order.setCreateTime(TimeHelper.nowInt());
                order.setChannelProductId(channelProductId);
                order.setItems(items);
                order.setDesc(desc);
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

    public Order orderCallback(Order order) {
        return orderDao.changeOrderCallback(order);
    }


    public Order orderProcessing(String orderId, String channelOrderId) {
        return orderDao.changeOrderProcessing(orderId, channelOrderId);
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
     * 检查订单是否重复
     *
     * @param channelOrderId
     * @return
     */
    public boolean putOrderId(String channelOrderId) {
        return this.redisTemplate.opsForZSet().addIfAbsent(CHANNEL_ORDER_TIME_TABLE_NAME, channelOrderId, TimeHelper.nowInt());
    }

    public Long removeChannelOrderSet(long expireTime) {
        return redisTemplate.opsForZSet().removeRangeByScore(CHANNEL_ORDER_TIME_TABLE_NAME, 0, expireTime);
    }

    /**
     * 清除过期订单，保留已回调但未完成发货的订单
     */
    public void clean() {
        int now = TimeHelper.nowInt();
        int expire = now - (int) TimeUnit.DAYS.toSeconds(60);

        long mongoDelCount = orderDao.deleteOrdersBeforeTimestampExceptShipping(expire);
        long mongoDelFlowCount = playerRechargeFlowDao.deleteOrdersBeforeTimestamp(expire);
        int channelOrderExpire = now - (int) TimeUnit.DAYS.toSeconds(7);

        Long removeChannelOrderCount = removeChannelOrderSet(channelOrderExpire);
        log.info("删除过期非发货中订单数量 mongoDelCount = {},mongoDelFlowCount={},removeChannelOrderCount = {}", mongoDelCount, mongoDelFlowCount, removeChannelOrderCount);
    }
}
