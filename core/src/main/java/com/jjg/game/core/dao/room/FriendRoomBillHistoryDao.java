package com.jjg.game.core.dao.room;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.redis.RedissonLock;
import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
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
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 好友房账单历史dao
 *
 * @author 2CL
 */
@Repository
public class FriendRoomBillHistoryDao extends MongoBaseDao<FriendRoomBillHistoryBean, Long> {

    @Autowired
    private RedisLock redisLock;

    public FriendRoomBillHistoryDao(@Autowired MongoTemplate mongoTemplate) {
        super(FriendRoomBillHistoryBean.class, mongoTemplate);
    }

    /**
     * 添加好友房历史记录
     */
    @RedissonLock(key = "#root.getPlayerBillLockKey(#historyBean.getRoomCreator())")
    public void addFriendRoomBillHistory(@Param("historyBean") FriendRoomBillHistoryBean historyBean) {
        mongoTemplate.save(historyBean);
    }

    /**
     * 好友房历史账单分页
     */
    public List<FriendRoomBillHistoryBean> pageFriendRoomBillHistory(long playerId, int pageIdx, int pageSize) {
        return mongoTemplate.find(
            Query.query(
                    Criteria.where("roomCreator").is(playerId)
                )
                .with(Pageable.ofSize(pageSize).withPage(pageIdx))
                .with(Sort.by("createdAt").descending()),
            FriendRoomBillHistoryBean.class
        );
    }

    /**
     * 根据ID查找账单数据
     */
    public FriendRoomBillHistoryBean getOneFriendRoomBillInfo(long billId) {
        return mongoTemplate.findOne(Query.query(Criteria.where("id").is(billId)), FriendRoomBillHistoryBean.class);
    }

    /**
     * 按游戏类型获取好友房账单历史
     */
    public List<GameBillResult> pageFriendRoomBillByGameType(long playerId, int pageIdx, int pageSize) {
        Aggregation aggregation =
            Aggregation.newAggregation(
                Aggregation.match(Criteria.where("roomCreator").is(playerId)),
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
     * 获取所有玩家所有未领取的收益
     *
     * @return 玩家所有收益奖励
     */
    public long getPlayerAllReward(long playerId) {
        Aggregation aggregation =
            Aggregation.newAggregation(
                Aggregation.match(Criteria.where("roomCreator").is(playerId).and("hasTookIncome").is(true)),
                Aggregation.group("roomCreator").sum("totalIncome").as("total")
            );
        AggregationResults<Long> results = mongoTemplate.aggregate(aggregation, "FriendBillHistoryBean", Long.class);
        List<Long> mappedResults = results.getMappedResults();
        return !mappedResults.isEmpty() ? mappedResults.getFirst() : 0;
    }

    /**
     * 更新所有未领奖的状态为已领取,仅在一键领取中调用，需要在调用上层进行加锁
     */
    public void updateAllHistoryRewardTook(long playerId) {
        // 更新玩家所有未领取的奖励状态
        mongoTemplate.updateMulti(Query.query(
                Criteria.where("roomCreator").is(playerId).and("hasTookIncome").is(false)
            ),
            Update.update("hasTookIncome", true),
            FriendRoomBillHistoryBean.class);
    }

    /**
     * 账单锁key，防止玩家一键领取时，有其他节点写入账单数据
     */
    public String getPlayerBillLockKey(long playerId) {
        return "FriendRoomBillUpdate:" + playerId;
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
