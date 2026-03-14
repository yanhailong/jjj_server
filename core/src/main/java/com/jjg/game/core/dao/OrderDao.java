package com.jjg.game.core.dao;

import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.OrderStatus;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/9/18 15:02
 */
@Repository
public class OrderDao extends MongoBaseDao<Order, Long> {
    public OrderDao(MongoTemplate mongoTemplate) {
        super(Order.class, mongoTemplate);
    }

    /**
     * 根据订单id获取订单对象
     *
     * @param id
     * @return
     */
    public Order getOrderById(String id) {
        return mongoTemplate.findOne(Query.query(Criteria.where("id").is(id)), Order.class);
    }

    public Order getOrderByUUid(String uuid) {
        return mongoTemplate.findOne(Query.query(Criteria.where("uuid").is(uuid)), Order.class);
    }

    /**
     * 订单置为成功状态
     *
     * @param orderId
     * @return
     */
    public Order changeOrderSuccess(String orderId, String channelOrderId) {
        return changeOrderStatus(orderId, OrderStatus.PROCESSING, OrderStatus.SUCCESS, channelOrderId);
    }


    /**
     * 订单置为失败状态
     *
     * @param order 订单数据
     * @return
     */
    public Order changeOrderCallback(Order order) {

        Query query = new Query(Criteria.where("id").is(order.getId()).and("orderStatus").is(OrderStatus.ORDER));
        Update update = new Update();
        update.set("orderStatus", OrderStatus.CALLBACK);
        update.set("updateTime", (int) (System.currentTimeMillis() / 1000));
        update.set("channelOrderId", order.getChannelOrderId());
        update.set("money", order.getMoney());
        update.set("regionCode", order.getRegionCode());
        update.set("channelProductId", order.getChannelProductId());

        return mongoTemplate.findAndModify(
                query,
                update,
                new FindAndModifyOptions().returnNew(true), // 返回更新后的文档
                Order.class
        );
    }

    /**
     * 订单置为收到处理订单状态
     *
     * @param orderId
     * @return
     */
    public Order changeOrderProcessing(String orderId, String channelOrderId) {
        return changeOrderStatus(orderId, OrderStatus.CALLBACK, OrderStatus.PROCESSING, channelOrderId);
    }

    /**
     * 订单置为失败状态
     *
     * @param orderId
     * @return
     */
    public Order changeOrderFail(String orderId) {
        Query query = new Query(Criteria.where("id").is(orderId).and("orderStatus").in(OrderStatus.ORDER, OrderStatus.CALLBACK));
        Update update = new Update();
        update.set("orderStatus", OrderStatus.FAIL);
        update.set("updateTime", (int) (System.currentTimeMillis() / 1000));
        return mongoTemplate.findAndModify(
                query,
                update,
                new FindAndModifyOptions().returnNew(true),
                Order.class
        );
    }

    /**
     * 修改订单状态
     *
     * @param orderId
     * @return
     */
    private Order changeOrderStatus(String orderId, OrderStatus exceptStatus, OrderStatus newStatus, String channelOrderId) {
        Query query = new Query(Criteria.where("id").is(orderId).and("orderStatus").is(exceptStatus));
        Update update = new Update();
        update.set("orderStatus", newStatus);
        update.set("updateTime", (int) (System.currentTimeMillis() / 1000));
        update.set("channelOrderId", channelOrderId);
        return mongoTemplate.findAndModify(
                query,
                update,
                new FindAndModifyOptions().returnNew(true), // 返回更新后的文档
                Order.class
        );
    }

    /**
     * 删除创建时间早于指定时间戳的订单
     *
     * @param timestamp 时间戳
     * @return 删除的订单数量
     */
    public long deleteOrdersBeforeTimestamp(int timestamp) {
        Query query = new Query(Criteria.where("createTime").lt(timestamp));
        return mongoTemplate.remove(query, Order.class).getDeletedCount();
    }

}
