package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimBaseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author 11
 * @date 2026/5/18
 */
@Repository
public class SimPlayerGameDao extends MongoBaseDao<SimBaseData, Long> {
    public SimPlayerGameDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimBaseData.class, mongoTemplate);
    }

    public void saveAll(Collection<SimBaseData> gameDataList) {
        if (gameDataList == null || gameDataList.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SimBaseData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SimBaseData data : gameDataList) {
            bulk.replaceOne(new Query(Criteria.where("_id").is(data.getPlayerId())), data, upsert);
        }
        bulk.execute();
    }
}
