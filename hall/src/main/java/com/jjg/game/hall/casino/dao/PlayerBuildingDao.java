package com.jjg.game.hall.casino.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.casino.data.CasinoInfo;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/18 09:38
 */
@Repository
public class PlayerBuildingDao extends MongoBaseDao<PlayerBuilding, Long> {
    public PlayerBuildingDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerBuilding.class, mongoTemplate);
    }

    public List<PlayerBuilding> findByPlayerId(Long playerId) {
        return mongoTemplate.find(
                Query.query(Criteria.where("playerId").is(playerId)),
                PlayerBuilding.class
        );
    }

    public PlayerBuilding findByPlayerIdAndCasinoId(Long playerId, int casinoId) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("playerId").is(playerId)
                        .and("casinoId").is(casinoId)),
                PlayerBuilding.class
        );
    }

}
