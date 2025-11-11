package com.jjg.game.core.dao.room;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.redis.RedissonLock;
import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.manager.SnowflakeManager;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 好友房账单历史dao
 *
 * @author 2CL
 */
@Repository
public class FriendRoomBillHistoryDao extends MongoBaseDao<FriendRoomBillHistoryBean, Long> {

    //    private Snowflake snowflake = new Snowflake(NodeType.GAME.getValue(), NodeType.GAME.getValue());
    private final SnowflakeManager snowflakeManager;

    public FriendRoomBillHistoryDao(@Autowired MongoTemplate mongoTemplate, @Lazy SnowflakeManager snowflakeManager) {
        super(FriendRoomBillHistoryBean.class, mongoTemplate);
        this.snowflakeManager = snowflakeManager;
    }

    /**
     * 添加好友房历史记录
     */
    @RedissonLock(key = "#root.getPlayerBillLockKey(#historyBean.getRoomCreator())")
    public void addFriendRoomBillHistory(@Param("historyBean") FriendRoomBillHistoryBean historyBean) {
        historyBean.setId(snowflakeManager.nextId());
        mongoTemplate.save(historyBean);
    }

    /**
     * 好友房历史账单分页
     */
    public List<FriendRoomBillHistoryBean> pageFriendRoomBillHistory(
            long playerId, int gameType, int pageIdx, int pageSize) {
        return mongoTemplate.find(
                Query.query(Criteria.where("roomCreator").is(playerId).and("gameType").is(gameType))
                        .with(Pageable.ofSize(pageSize).withPage(pageIdx))
                        .with(Sort.by("createdAt").descending()),
                FriendRoomBillHistoryBean.class
        );
    }


    public static class MonthStatisticsDto {
        // 月份
        public int month;
        // 总数
        public int count;
    }

    /**
     * 获取月份统计
     */
    public List<MonthStatisticsDto> monthStatistic(long playerId, int gameType, List<Integer> month) {
        if (month.isEmpty()) {
            return new ArrayList<>();
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("roomCreator").is(playerId)
                                .and("gameType").is(gameType)
                                .and("month").in(month)),
                Aggregation.group("month").count().as("count"),
                Aggregation.project()
                        .and("_id").as("month")
                        .and("count").as("count")
        );
        AggregationResults<MonthStatisticsDto> results =
                mongoTemplate.aggregate(aggregation, "friendRoomBillHistoryBean", MonthStatisticsDto.class);
        return results.getMappedResults();
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
        if (pageSize < 1) {
            pageSize = 5;
        }

        Aggregation aggregation =
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("roomCreator").is(playerId)),
                        Aggregation.group("gameType")
                                .sum("totalIncome").as("totalIncome")
                                .sum("totalFlowing").as("totalWin")
                                .sum(ConditionalOperators.when(
                                                Criteria.where("hasTookIncome").is(false)
                                        )
                                        .thenValueOf("totalIncome").otherwise(0)).as("totalIncomeCanTake")
                                .count().as("totalRound"),
                        Aggregation.project()
                                .and("_id").as("gameType")
                                .and("totalIncome").as("totalIncome")
                                .and("totalWin").as("totalWin")
                                .and("totalRound").as("totalRound")
                                .and("totalIncomeCanTake").as("totalIncomeCanTake")
                                .andExclude("_id"),
                        Aggregation.skip((long) pageIdx * pageSize),
                        Aggregation.limit(pageSize)
                );
        AggregationResults<GameBillResult> results =
                mongoTemplate.aggregate(aggregation, "friendRoomBillHistoryBean", GameBillResult.class);
        return results.getMappedResults();
    }

    /**
     * 获取所有玩家所有未领取的收益
     *
     * @return 玩家所有收益奖励
     */
    public List<Item> getPlayerAllReward(long playerId) {
        Aggregation aggregation =
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("roomCreator").is(playerId).and("hasTookIncome").is(false)),
                        Aggregation.group("itemId")
                                .sum("totalIncome").as("totalIncome"),
                        Aggregation.project()
                                .and("_id").as("id")
                                .and("totalIncome").as("itemCount")
                                .andExclude("_id")
                );
        AggregationResults<Document> rawResults = mongoTemplate.aggregate(aggregation, "friendRoomBillHistoryBean", Document.class);
        //手动拼接
        List<Item> items = new ArrayList<>();
        for (Document doc : rawResults.getMappedResults()) {
            items.add(JSONObject.parseObject(doc.toJson(), Item.class));
        }
        return items;
    }

    /**
     * 更新所有未领奖的状态为已领取,仅在一键领取中调用，需要在调用上层进行加锁
     */
    public void updateAllHistoryRewardTook(long playerId) {
        // 更新玩家所有未领取的奖励状态
        mongoTemplate.updateMulti(Query.query(
                        Criteria.where("roomCreator").is(playerId).and("hasTookIncome").is(false)),
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
        private int gameType;
        // 总赢分
        private int totalWin;
        // 总收益
        private int totalIncome;
        // 可以领取的总收益
        private int totalIncomeCanTake;
        // 总对局数
        private int totalRound;

        public int getGameType() {
            return gameType;
        }

        public void setGameType(int gameType) {
            this.gameType = gameType;
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
