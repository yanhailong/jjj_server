package com.jjg.game.slots.game.superstar.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.superstar.data.SuperStarResultLib;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 财神
 */
@Repository
public class SuperStarResultLibDao extends AbstractResultLibDao<SuperStarResultLib> {

    public SuperStarResultLibDao(@Autowired MongoTemplate mongoTemplate) {
        super(SuperStarResultLib.class, mongoTemplate);
    }

}
