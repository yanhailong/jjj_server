package com.jjg.game.core.dao;

import com.jjg.game.core.data.FriendBillHistoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 好友房账单历史dao
 *
 * @author 2CL
 */
@Repository
public class FriendRoomBillHistoryDao extends MongoBaseDao<FriendBillHistoryBean, Long> {

    public FriendRoomBillHistoryDao(@Autowired MongoTemplate mongoTemplate) {
        super(FriendBillHistoryBean.class, mongoTemplate);
    }

    /**
     * 添加好友房历史记录
     */
    public void addFriendRoomBillHistory(FriendBillHistoryBean historyBean) {
        mongoTemplate.save(historyBean);
    }

    /**
     * 好友房历史账单分页
     */
    public List<FriendBillHistoryBean> pageFriendRoomBillHistory(long playerId, int pageIdx, int pageSize) {
        return mongoTemplate.find(
            Query.query(
                    Criteria.where("playerId").is(playerId)
                )
                .with(Pageable.ofSize(pageSize).withPage(pageIdx))
                .with(Sort.by("createdAt").descending()),
            FriendBillHistoryBean.class
        );
    }

    /**
     * 按游戏类型获取好友房账单历史
     */
    public List<GameBillResult> pageFriendRoomBillByGameType(long playerId, int pageIdx, int pageSize) {
        Aggregation aggregation =
            Aggregation.newAggregation(
                Aggregation.match(Criteria.where("playerId").is(playerId)),
                Aggregation.group("gameType")
                    .sum("totalIncome").as("totalIncome")
                    .sum("totalFollowing").as("totalWin")
                    .sum(ConditionalOperators.when(
                            Criteria.where("hasTookIncome").is(false)
                        )
                        .thenValueOf("totalIncome").otherwise(0)).as("totalIncomeCanTake")
                    .count().as("totalRound"),
                Aggregation.skip((long) pageIdx * pageSize),
                Aggregation.limit(pageSize)
            );
        AggregationResults<GameBillResult> results =
            mongoTemplate.aggregate(aggregation, "FriendBillHistoryBean", GameBillResult.class);
        return results.getMappedResults();
    }

    /**
     * 更新所有未领奖的状态为已领取
     */
    public void updateAllHistoryRewardTook(long playerId) {
        // 更新玩家所有未领取的奖励状态
        mongoTemplate.updateMulti(Query.query(
                Criteria.where("playerId").is(playerId).and("hasTookIncome").is(false)
            ),
            Update.update("hasTookIncome", true),
            FriendBillHistoryBean.class);
    }

    /**
     * 游戏账单结果集
     */
    public static class GameBillResult {
        // ID
        private int id;
        // 总赢分
        private int totalWin;
        // 总收益
        private int totalIncome;
        // 可以领取的总收益
        private int totalIncomeCanTake;
        // 总对局数
        private int totalRound;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getTotalWin() {
            return totalWin;
        }

        public void setTotalWin(int totalWin) {
            this.totalWin = totalWin;
        }

        public int getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(int totalIncome) {
            this.totalIncome = totalIncome;
        }

        public int getTotalIncomeCanTake() {
            return totalIncomeCanTake;
        }

        public void setTotalIncomeCanTake(int totalIncomeCanTake) {
            this.totalIncomeCanTake = totalIncomeCanTake;
        }

        public int getTotalRound() {
            return totalRound;
        }

        public void setTotalRound(int totalRound) {
            this.totalRound = totalRound;
        }
    }
}
