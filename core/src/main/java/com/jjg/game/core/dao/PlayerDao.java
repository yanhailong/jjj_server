package com.jjg.game.core.dao;

import com.jjg.game.core.data.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/5/26 13:31
 */
@Repository
public class PlayerDao extends MongoBaseDao<Player,Long>{
    public PlayerDao(@Autowired MongoTemplate mongoTemplate) {
        super(Player.class, mongoTemplate);
    }

}
