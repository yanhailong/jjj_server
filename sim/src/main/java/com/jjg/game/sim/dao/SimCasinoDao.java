package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimCasinoData;
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
 * @date 2026/5/26
 */
@Repository
public class SimCasinoDao extends MongoBaseDao<SimCasinoData, String> {
    public SimCasinoDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimCasinoData.class, mongoTemplate);
    }

    /**
     * 按 playerId 加载所有赌场
     */
    public List<SimCasinoData> findByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.find(query, SimCasinoData.class);
    }

    public void saveAll(Collection<SimCasinoData> simCasinoDataList) {
        if (simCasinoDataList == null || simCasinoDataList.isEmpty()) {
            return;
        }
        BulkOperations bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SimCasinoData.class);
        FindAndReplaceOptions upsert = FindAndReplaceOptions.options().upsert();
        for (SimCasinoData data : simCasinoDataList) {
            data.buildKey();
            bulk.replaceOne(new Query(Criteria.where("_id").is(data.getId())), data, upsert);
        }
        bulk.execute();
    }

    @Override
    public <S extends SimCasinoData> S save(S entity) {
        entity.buildKey();
        return super.save(entity);
    }
}
