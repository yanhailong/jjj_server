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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 按 playerId 加载所有场景
     */
    public List<SimCasinoData> findByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.find(query, SimCasinoData.class);
    }

    /**
     * 按联合主键加载单个场景 (内存仅保留当前场景, 切换/校验时按需读取)
     */
    public SimCasinoData findOne(long playerId, int casinoId) {
        return mongoTemplate.findById(SimCasinoData.buildKey(playerId, casinoId), SimCasinoData.class);
    }

    /**
     * 玩家是否已拥有任意场景 (判定新老玩家)
     */
    public boolean existsByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.exists(query, SimCasinoData.class);
    }

    /**
     * 任取玩家的一个场景 (currentCasinoId 失效时回退)
     */
    public SimCasinoData findFirstByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId)).limit(1);
        return mongoTemplate.findOne(query, SimCasinoData.class);
    }

    /**
     * 玩家已拥有的场景 id 列表 (只取 casinoId 字段, 用于下发拥有列表)
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

    /**
     * 批量查询玩家最高赌场等级，排行榜展示使用，避免逐玩家查询。
     */
    public Map<Long, Integer> findMaxCasinoLevel(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Query query = Query.query(Criteria.where("playerId").in(playerIds));
        query.fields().include("playerId", "casinoLevel");
        Map<Long, Integer> result = new HashMap<>();
        for (SimCasinoData data : mongoTemplate.find(query, SimCasinoData.class)) {
            result.merge(data.getPlayerId(), data.getCasinoLevel(), Math::max);
        }
        return result;
    }

    /**
     * 批量读取随机候选的赌场简要数据，不拉建筑/游客等大字段。
     */
    public List<SimCasinoData> findVisitBriefs(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyList();
        }
        Query query = Query.query(Criteria.where("playerId").in(playerIds));
        query.fields().include("playerId", "casinoId", "casinoLevel");
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
