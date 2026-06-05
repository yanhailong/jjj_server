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

import java.util.ArrayList;
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

    /**
     * 按联合主键加载单个赌场 (内存仅保留当前赌场, 切换/校验时按需读取)
     */
    public SimCasinoData findOne(long playerId, int casinoId) {
        return mongoTemplate.findById(SimCasinoData.buildKey(playerId, casinoId), SimCasinoData.class);
    }

    /**
     * 玩家是否已拥有任意赌场 (判定新老玩家)
     */
    public boolean existsByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.exists(query, SimCasinoData.class);
    }

    /**
     * 任取玩家的一个赌场 (currentCasinoId 失效时回退)
     */
    public SimCasinoData findFirstByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId)).limit(1);
        return mongoTemplate.findOne(query, SimCasinoData.class);
    }

    /**
     * 玩家已拥有的赌场 id 列表 (只取 casinoId 字段, 用于下发拥有列表)
     */
    public List<Integer> findCasinoIdsByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        query.fields().include("casinoId");
        List<SimCasinoData> list = mongoTemplate.find(query, SimCasinoData.class);
        List<Integer> ids = new ArrayList<>(list.size());
        for (SimCasinoData data : list) {
            ids.add(data.getCasinoId());
        }
        return ids;
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
