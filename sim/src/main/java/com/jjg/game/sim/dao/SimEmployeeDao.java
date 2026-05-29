package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimEmployeeData;
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
 * @author 11
 * @date 2026/5/28
 */
@Repository
public class SimEmployeeDao extends MongoBaseDao<SimEmployeeData, String> {

    public SimEmployeeDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimEmployeeData.class, mongoTemplate);
    }

    public List<SimEmployeeData> findByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.find(query, SimEmployeeData.class);
    }

    public void saveAll(Collection<SimEmployeeData> simEmployeeDataList) {
        if (simEmployeeDataList == null || simEmployeeDataList.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SimEmployeeData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SimEmployeeData data : simEmployeeDataList) {
            data.buildKey();
            bulk.replaceOne(new Query(Criteria.where("_id").is(data.getId())), data, upsert);
        }
        bulk.execute();
    }

    @Override
    public <S extends SimEmployeeData> S save(S entity) {
        entity.buildKey();
        return super.save(entity);
    }
}
