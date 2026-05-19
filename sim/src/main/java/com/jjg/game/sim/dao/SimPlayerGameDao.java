package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimPlayerGameData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2026/5/18
 */
@Repository
public class SimPlayerGameDao extends MongoBaseDao<SimPlayerGameData,Long> {
    public SimPlayerGameDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimPlayerGameData.class, mongoTemplate);
    }
}
