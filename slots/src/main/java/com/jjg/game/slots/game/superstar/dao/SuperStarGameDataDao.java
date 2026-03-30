package com.jjg.game.slots.game.superstar.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.superstar.data.SuperStarPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 超级明星
 */
@Repository
public class SuperStarGameDataDao extends AbstractGameDataDao<SuperStarPlayerGameDataDTO> {

    public SuperStarGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(SuperStarPlayerGameDataDTO.class, mongoTemplate);
    }

}