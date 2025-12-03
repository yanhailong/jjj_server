package com.jjg.game.hall.casino.dao;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/18 09:38
 */
@Repository
public class PlayerBuildingDao extends MongoBaseDao<PlayerBuilding, Long> {
    private final Logger log = LoggerFactory.getLogger(PlayerBuildingDao.class);

    public PlayerBuildingDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerBuilding.class, mongoTemplate);
    }

    public List<PlayerBuilding> findByPlayerId(long playerId) {
        return mongoTemplate.find(
                Query.query(Criteria.where("playerId").is(playerId)),
                PlayerBuilding.class
        );
    }

    public PlayerBuilding findByPlayerIdAndCasinoId(long playerId, int casinoId) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("playerId").is(playerId)
                        .and("casinoId").is(casinoId)),
                PlayerBuilding.class
        );
    }

    public void batchUpdateByPlayerIdAndCasinoId(List<PlayerBuilding> playerBuildings) {
        if (CollectionUtil.isEmpty(playerBuildings)) {
            return;
        }
        try {
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PlayerBuilding.class);
            for (PlayerBuilding building : playerBuildings) {
                if (building == null) {
                    continue;
                }
                Query query = Query.query(
                        Criteria.where("playerId").is(building.getPlayerId())
                                .and("casinoId").is(building.getCasinoId())
                );
                Update update = new Update().set("casinoInfo", building.getCasinoInfo());
                bulkOps.upsert(query, update);
            }
            bulkOps.execute();
        } catch (Exception e) {
            log.error("保存玩家建筑数据失败 playerId:{}", playerBuildings.getFirst().getPlayerId());
        }
    }
}
