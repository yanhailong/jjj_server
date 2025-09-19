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
     * @param id
     * @return
     */
    public Order getOrderById(long id) {
        return mongoTemplate.findOne(Query.query(Criteria.where("id").is(id)), Order.class);
    }

    /**
     * 订单置为成功状态
     * @param orderId
     * @return
     */
    public Order changeOrderSuccess(String orderId) {
        return changeOrderStatus(orderId,OrderStatus.ORDER,OrderStatus.SUCCESS);
    }

    /**
     * 修改订单状态
     * @param orderId
     * @return
     */
    private Order changeOrderStatus(String orderId,OrderStatus exceptStatus,OrderStatus newStatus) {
        Query query = new Query(Criteria.where("id").is(orderId).and("orderStatus").is(exceptStatus));
        Update update = new Update();
        update.set("orderStatus", newStatus);
        return mongoTemplate.findAndModify(
                query,
                update,
                new FindAndModifyOptions().returnNew(true), // 返回更新后的文档
                Order.class
        );
    }
}
