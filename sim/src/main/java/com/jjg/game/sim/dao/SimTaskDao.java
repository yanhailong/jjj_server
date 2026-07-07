package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimTaskData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

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
