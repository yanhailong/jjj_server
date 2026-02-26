package com.jjg.game.core.dao;

import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.PlayerRechargeFlow;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * @author lm
 * @date 2026/2/25 15:09
 */
@Repository
public class PlayerRechargeFlowDao extends MongoBaseDao<PlayerRechargeFlow, String> {
    public PlayerRechargeFlowDao(MongoTemplate mongoTemplate) {
        super(PlayerRechargeFlow.class, mongoTemplate);
    }

    /**
     * 记录一条充值流水
     */
    public void addRechargeFlow(Order order) {
        if (order == null || StringUtils.isEmpty(order.getId()) || order.getPrice() == null) {
            return;
        }
        long rechargeTime = order.getUpdateTime() > 0 ? order.getUpdateTime() * 1000L : System.currentTimeMillis();
        addRechargeFlow(order.getId(), order.getPlayerId(), order.getPrice(), rechargeTime, order.getPayChannel());
    }

    /**
     * 记录一条充值流水
     *
     * @param rechargeTime 毫秒
     */
    private void addRechargeFlow(String orderId, long playerId, BigDecimal amount, long rechargeTime, int channelId) {
        Query query = Query.query(Criteria.where("orderId").is(orderId));
        Update update = new Update()
                .setOnInsert("orderId", orderId)
                .setOnInsert("playerId", playerId)
                .setOnInsert("channelId", channelId)
                .setOnInsert("amount", amount)
                .setOnInsert("rechargeTime", rechargeTime);
        mongoTemplate.upsert(query, update, clazz);
    }

    /**
     * 查询玩家在时间区间内的充值流水
     *
     * @param startTime 开始时间(毫秒)
     * @param endTime   结束时间(毫秒)
     */
    public List<PlayerRechargeFlow> queryByPlayerIdAndTimeRange(long playerId, int channelId, long startTime, long endTime) {
        if (playerId <= 0) {
            return Collections.emptyList();
        }
        Criteria criteria = Criteria.where("playerId").is(playerId)
                .and("rechargeTime").gte(startTime).lt(endTime);
        if (channelId > 0) {
            criteria.and("channelId").is(channelId);
        }
        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "rechargeTime"));
        return mongoTemplate.find(query, clazz);
    }

    /**
     * 查询玩家在时间区间内的充值总额
     *
     * @param startTime 开始时间(毫秒)
     * @param endTime   结束时间(毫秒)
     */
    public BigDecimal sumAmountByPlayerIdAndTimeRange(long playerId, int channelId, long startTime, long endTime) {
        if (playerId <= 0) {
            return BigDecimal.ZERO;
        }
        Criteria criteria = Criteria.where("playerId").is(playerId)
                .and("rechargeTime").gte(startTime).lt(endTime);
        if (channelId > 0) {
            criteria.and("channelId").is(channelId);
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("playerId").sum("amount").as("totalAmount")
        );
        AggregationResults<RechargeTotalResult> results =
                mongoTemplate.aggregate(aggregation, mongoTemplate.getCollectionName(clazz), RechargeTotalResult.class);
        RechargeTotalResult result = results.getUniqueMappedResult();
        if (result == null || result.totalAmount == null) {
            return BigDecimal.ZERO;
        }
        return result.totalAmount;
    }

    private static class RechargeTotalResult {
        private BigDecimal totalAmount;

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

}
