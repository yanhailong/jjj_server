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

import java.util.*;

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
     * 特邀游客红点只读取刷新次数和待邀请道具，避免登录红点请求加载完整场景数据。
     */
    public SimCasinoData findSpecialGuestRedDotData(long playerId, int casinoId) {
        Query query = casinoId > 0
                ? Query.query(Criteria.where("_id").is(SimCasinoData.buildKey(playerId, casinoId)))
                : Query.query(Criteria.where("playerId").is(playerId)).limit(1);
        query.fields().include("playerId", "casinoId", "specialGuestNextRefreshTime",
                "specialGuestRefreshCount", "specialGuestItemCounts");
        SimCasinoData data = mongoTemplate.findOne(query, SimCasinoData.class);
        if (data != null || casinoId <= 0) {
            return data;
        }
        query = Query.query(Criteria.where("playerId").is(playerId)).limit(1);
        query.fields().include("playerId", "casinoId", "specialGuestNextRefreshTime",
                "specialGuestRefreshCount", "specialGuestItemCounts");
        return mongoTemplate.findOne(query, SimCasinoData.class);
    }

    /**
     * 游客升星红点只读取当前场景的游客数据，避免加载建筑等无关字段。
     */
    public SimCasinoData findGuestGrowthRedDotData(long playerId, int casinoId) {
        Query query = casinoId > 0
                ? Query.query(Criteria.where("_id").is(SimCasinoData.buildKey(playerId, casinoId)))
                : Query.query(Criteria.where("playerId").is(playerId)).limit(1);
        query.fields().include("playerId", "casinoId", "guestMap");
        SimCasinoData data = mongoTemplate.findOne(query, SimCasinoData.class);
        if (data != null || casinoId <= 0) {
            return data;
        }
        query = Query.query(Criteria.where("playerId").is(playerId)).limit(1);
        query.fields().include("playerId", "casinoId", "guestMap");
        return mongoTemplate.findOne(query, SimCasinoData.class);
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
