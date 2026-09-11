package com.jjg.game.sim.dao;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimTaskData;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sim 任务数据 DAO (主键 = playerId)。
 *
 * @author 11
 * @date 2026/6/25
 */
@Repository
public class SimTaskDao extends MongoBaseDao<SimTaskData, Long> {
    public SimTaskDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimTaskData.class, mongoTemplate);
    }

    /**
     * 拜访展示只需要展示勋章列表, 投影避免拉全量任务文档。
     */
    public List<Integer> findDisplayedMedalIds(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().include("displayedMedalIds");
        SimTaskData data = mongoTemplate.findOne(query, SimTaskData.class);
        return data == null ? List.of() : data.getDisplayedMedalIds();
    }

    /** 个人卡片解析展示徽章档位，只读取展示列表和成就进度。 */
    public SimTaskData findMedalDisplayData(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().include("displayedMedalIds").include("achievementTasks");
        return mongoTemplate.findOne(query, SimTaskData.class);
    }

    /**
     * 统计主线和成就中当前可领取奖励的任务数，只返回计数，不拉取任务文档。
     */
    public Map<Integer, Integer> findClaimableCounts(long playerId) {
        Document achievements = new Document("$objectToArray",
                new Document("$ifNull", List.of("$achievementTasks", new Document())));
        Document claimableAchievements = new Document("$filter", new Document("input", achievements)
                .append("as", "task")
                .append("cond", new Document("$eq", List.of("$$task.v.status",
                        TaskConstant.TaskStatus.STATUS_COMPLETED))));
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(playerId)),
                context -> new Document("$project", new Document("main",
                        new Document("$cond", List.of(
                                new Document("$eq", List.of("$mainTask.status",
                                        TaskConstant.TaskStatus.STATUS_COMPLETED)), 1, 0)))
                        .append("achievement", new Document("$size", claimableAchievements))));
        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, SimTaskData.class, Document.class);
        Document result = results.getUniqueMappedResult();
        if (result == null) {
            return Map.of(
                    TaskConstant.TaskType.MAIN_LINE, 0,
                    TaskConstant.TaskType.ACHIEVEMENT, 0);
        }
        return Map.of(
                TaskConstant.TaskType.MAIN_LINE, result.getInteger("main", 0),
                TaskConstant.TaskType.ACHIEVEMENT, result.getInteger("achievement", 0));
    }

    public void saveAll(Collection<SimTaskData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SimTaskData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SimTaskData data : list) {
            bulk.replaceOne(new Query(Criteria.where("_id").is(data.getPlayerId())), data, upsert);
        }
        bulk.execute();
    }
}
