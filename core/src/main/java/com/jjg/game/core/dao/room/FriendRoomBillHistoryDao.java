package com.jjg.game.core.dao.room;

import com.jjg.game.common.redis.RedissonLock;
import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
import com.jjg.game.core.data.FriendRoomRewardItem;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.manager.SnowflakeManager;
import com.mongodb.bulk.BulkWriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友房账单历史dao
 *
 * @author 2CL
 */
@Repository
public class FriendRoomBillHistoryDao extends MongoBaseDao<FriendRoomBillHistoryBean, Long> {
    private final Logger log = LoggerFactory.getLogger(FriendRoomBillHistoryDao.class);

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

        // 构建条件表达式
        ConditionalOperators.Cond totalIncomeCanTakeCond = ConditionalOperators
                .when(Criteria.where("gameMajorType").is(1))
                .then(
                        ArithmeticOperators.Subtract
                                .valueOf("totalIncome")
                                .subtract(
                                        ConditionalOperators.ifNull("hasReceivedIncome").then(0)
                                )
                )
                .otherwise(
                        ConditionalOperators
                                .when(Criteria.where("hasTookIncome").is(false))
                                .thenValueOf("totalIncome")
                                .otherwise(0)
                );

        Aggregation aggregation =
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("roomCreator").is(playerId)),
                        Aggregation.group("gameType")
                                .sum("totalIncome").as("totalIncome")
                                .sum("totalFlowing").as("totalWin")
                                .sum(totalIncomeCanTakeCond).as("totalIncomeCanTake")
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
    public List<FriendRoomRewardItem> getPlayerAllReward(long playerId) {
        // 构建聚合管道
        Aggregation aggregation = Aggregation.newAggregation(
                // 第一步：匹配条件
                Aggregation.match(
                        new Criteria().andOperator(
                                Criteria.where("roomCreator").is(playerId),
                                new Criteria().orOperator(
                                        Criteria.where("gameMajorType").is(1),
                                        new Criteria().andOperator(
                                                Criteria.where("gameMajorType").ne(1),
                                                Criteria.where("hasTookIncome").is(false)
                                        )
                                )
                        )
                ),

                // 第二步：为每个文档计算实际收益 - 更安全地处理字段
                Aggregation.project()
                        .and("_id").as("id")  // 数据库的_id
                        .and("itemId").as("itemId")
                        .and("gameMajorType").as("gameMajorType")
                        .and("totalIncome").as("totalIncome")
                        // 使用表达式安全处理hasReceivedIncome字段
                        .andExpression("ifNull(hasReceivedIncome, 0)").as("safeHasReceivedIncome")
                        .andExpression(
                                "cond(eq(gameMajorType, 1), " +
                                        "subtract(ifNull(totalIncome, 0), ifNull(hasReceivedIncome, 0)), " +
                                        "ifNull(totalIncome, 0))"
                        ).as("count")
        );

        AggregationResults<FriendRoomRewardItem> rawResults = mongoTemplate.aggregate(
                aggregation, "friendRoomBillHistoryBean", FriendRoomRewardItem.class
        );

        List<FriendRoomRewardItem> queryList = rawResults.getMappedResults();
        if(queryList.isEmpty()) {
            return queryList;
        }

        List<FriendRoomRewardItem> tmpList = new ArrayList<>(queryList);
        tmpList.removeIf(item -> item.getGameMajorType() == 1 && item.getCount() < 1);
        return tmpList;
    }

    /**
     * 更新玩家所有未领取的奖励状态（批量操作）
     */
    public boolean updateAllHistoryRewardTook(long playerId, List<FriendRoomRewardItem> playerAllReward) {
        if (playerAllReward == null || playerAllReward.isEmpty()) {
            return true;
        }

        try {
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, FriendRoomBillHistoryBean.class);

            for (FriendRoomRewardItem reward : playerAllReward) {
                long docId = reward.getId();
                long count = reward.getCount();
                int gameMajorType = reward.getGameMajorType();

                Query query = Query.query(Criteria.where("_id").is(docId));
                Update update = new Update();

                if (gameMajorType != 1) {
                    // gameMajorType不为1
                    update.set("hasTookIncome", true);
                } else {
                    // gameMajorType为1
                    update.inc("hasReceivedIncome", count);
                }
                bulkOps.updateOne(query, update);
            }

            // 执行批量操作
            BulkWriteResult result = bulkOps.execute();
            log.info("批量更新完成，玩家：{}，匹配记录数：{}，修改记录数：{}",playerId, result.getMatchedCount(), result.getModifiedCount());
            return true;
        } catch (Exception e) {
            log.error("批量更新玩家奖励状态失败，playerId: {}", playerId, e);
            return false;
        }
    }

    /**
     * 账单锁key，防止玩家一键领取时，有其他节点写入账单数据
     */
    public String getPlayerBillLockKey(long playerId) {
        return "FriendRoomBillUpdate:" + playerId;
    }

    /**
     * 保存slots账单
     * @param historyBean
     */
    public void saveSlotsBillHistory(FriendRoomBillHistoryBean historyBean) {
        Query query = Query.query(Criteria.where("_id").is(historyBean.getId()));

        Update update = new Update();
        update.set("gameType", historyBean.getGameType())
                .set("roomCreator", historyBean.getRoomCreator())
                .inc("totalFlowing", historyBean.getTotalFlowing())
                .inc("totalIncome", historyBean.getTotalIncome())
                .set("itemId", historyBean.getItemId())
                .set("month", historyBean.getMonth())
                .set("createdAt", historyBean.getCreatedAt())
                .set("hasTookIncome", false)
                .set("gameMajorType", historyBean.getGameMajorType());

        updateMapField(update, "partInPlayerIncome", historyBean.getPartInPlayerIncome());
        updateMapField(update, "partInPlayerBetScore", historyBean.getPartInPlayerBetScore());

        mongoTemplate.findAndModify(query, update,new FindAndModifyOptions().returnNew(true).upsert(true), FriendRoomBillHistoryBean.class);
    }

    private void updateMapField(Update update, String fieldName, Map<Long, Long> map) {
        if (map != null && !map.isEmpty()) {
            // 逐个字段更新
            for (Map.Entry<Long, Long> entry : map.entrySet()) {
                String mapField = fieldName + "." + entry.getKey().toString();
                update.inc(mapField, entry.getValue());
            }
        }
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
