package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimBaseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author 11
 * @date 2026/5/18
 */
@Repository
public class SimPlayerGameDao extends MongoBaseDao<SimBaseData, Long> {
    private static final long VISIT_MAX_ID_CACHE_MILLIS = 60_000L;

    private volatile long visitMaxPlayerId;
    private volatile long visitMaxPlayerIdExpireTime;

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

    /**
     * 按主键游标读取少量随机候选，避免全表 count/skip/sample。
     */
    public List<Long> findVisitCandidateIds(long afterPlayerId, int limit, Collection<Long> excludes) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Criteria criteria = Criteria.where("_id").gt(afterPlayerId);
        if (excludes != null && !excludes.isEmpty()) {
            criteria.nin(excludes);
        }
        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.ASC, "_id"))
                .limit(limit);
        query.fields().include("playerId");
        return mongoTemplate.find(query, SimBaseData.class).stream()
                .map(SimBaseData::getPlayerId).toList();
    }

    /**
     * 缓存当前最大玩家 id，供随机游标限定在真实雪花 id 区间内。
     * 查询只使用 _id 索引且每分钟最多执行一次。
     */
    public long findVisitMaxPlayerId() {
        long now = System.currentTimeMillis();
        if (visitMaxPlayerId > 0 && now < visitMaxPlayerIdExpireTime) {
            return visitMaxPlayerId;
        }
        synchronized (this) {
            if (visitMaxPlayerId > 0 && now < visitMaxPlayerIdExpireTime) {
                return visitMaxPlayerId;
            }
            Query query = new Query().with(Sort.by(Sort.Direction.DESC, "_id")).limit(1);
            query.fields().include("playerId");
            SimBaseData latest = mongoTemplate.findOne(query, SimBaseData.class);
            visitMaxPlayerId = latest == null ? 0 : latest.getPlayerId();
            visitMaxPlayerIdExpireTime = now + VISIT_MAX_ID_CACHE_MILLIS;
            return visitMaxPlayerId;
        }
    }

    /**
     * 拜访展示只需要玩家总等级, 投影避免拉全量玩家文档。
     */
    public int findAllLevelById(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().include("allLevel");
        SimBaseData data = mongoTemplate.findOne(query, SimBaseData.class);
        return data == null ? 0 : data.getAllLevel();
    }

    public List<SimBaseData> findVisitBriefs(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyList();
        }
        Query query = Query.query(Criteria.where("_id").in(playerIds));
        query.fields().include("playerId", "allLevel", "currentCasinoId");
        return mongoTemplate.find(query, SimBaseData.class);
    }
}
