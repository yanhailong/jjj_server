package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimCoopTaskData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * 多人协作任务数据 DAO (主键 = playerId, 范式对齐 {@link SimTaskDao})。
 *
 * @author 11
 * @date 2026/7/6
 */
@Repository
public class SimCoopTaskDao extends MongoBaseDao<SimCoopTaskData, Long> {
    public SimCoopTaskDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimCoopTaskData.class, mongoTemplate);
    }

    public void saveAll(Collection<SimCoopTaskData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SimCoopTaskData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SimCoopTaskData data : list) {
            bulk.replaceOne(new Query(Criteria.where("_id").is(data.getPlayerId())), data, upsert);
        }
        bulk.execute();
    }
}
