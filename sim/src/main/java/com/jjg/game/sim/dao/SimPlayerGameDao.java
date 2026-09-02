package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimBaseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

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

    /**
     * 纯随机抽取少量玩家。$sample 必须保持为第一阶段，才能由 MongoDB 使用随机游标。
     */
    public List<SimBaseData> findRandomVisitCandidates(int sampleSize) {
        if (sampleSize <= 0) {
            return List.of();
        }
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.sample(sampleSize),
                Aggregation.project("playerId", "currentCasinoId"));
        return mongoTemplate.aggregate(aggregation, SimBaseData.class, SimBaseData.class)
                .getMappedResults();
    }

    /**
     * 经营信息-完成任务数 +1 (玩家不在本节点在线时的落库兜底)。
     */
    public void incrementFinishedTaskCount(long playerId) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(playerId)),
                new Update().inc("finishedTaskCount", 1), SimBaseData.class);
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

    /**
     * 特邀游客红点只需要当前场景和广告状态，避免登录红点请求加载完整经营数据。
     */
    public SimBaseData findSpecialGuestRedDotData(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().include("playerId", "currentCasinoId", "specialGuestAdCfgIds",
                "specialGuestAdRefreshDay", "specialGuestAdCdEndTime", "specialGuestAdNextRefreshTime");
        return mongoTemplate.findOne(query, SimBaseData.class);
    }

    /**
     * 游客成长红点只需要当前场景 id，避免登录红点请求加载完整经营数据。
     */
    public int findCurrentCasinoId(long playerId) {
        Query query = Query.query(Criteria.where("_id").is(playerId));
        query.fields().include("currentCasinoId");
        SimBaseData data = mongoTemplate.findOne(query, SimBaseData.class);
        return data == null ? 0 : data.getCurrentCasinoId();
    }

}
