package com.jjg.game.core.dao;

import com.jjg.game.core.data.PlayerPack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/7 15:14
 */
@Repository
public class PlayerPackDao extends MongoBaseDao<PlayerPack,Long>{
    public PlayerPackDao(@Autowired MongoTemplate mongoTemplate) {
        super(PlayerPack.class, mongoTemplate);
    }

    public PlayerPack findById(long id) {
        return mongoTemplate.findById(id, PlayerPack.class);
    }
}
