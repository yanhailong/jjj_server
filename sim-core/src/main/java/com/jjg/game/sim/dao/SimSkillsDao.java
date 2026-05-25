package com.jjg.game.sim.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.sim.data.SimSkillsData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/22
 */
@Repository
public class SimSkillsDao extends MongoBaseDao<SimSkillsData, String> {
    public SimSkillsDao(@Autowired MongoTemplate mongoTemplate) {
        super(SimSkillsData.class, mongoTemplate);
    }

    public List<SimSkillsData> findByPlayerId(long playerId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("playerId").is(playerId));
        return mongoTemplate.find(query, SimSkillsData.class);
    }
}
