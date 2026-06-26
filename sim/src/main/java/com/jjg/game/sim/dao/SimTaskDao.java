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
