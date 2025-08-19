package com.jjg.game.hall.casino.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/8/18 09:38
 */
@Repository
public class PlayerBuildingDao extends MongoBaseDao<PlayerBuilding,Long> {
    public PlayerBuildingDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerBuilding.class, mongoTemplate);
    }

    public PlayerBuilding findById(long id) {
        return mongoTemplate.findById(id, PlayerBuilding.class);
    }
}
