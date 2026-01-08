package com.jjg.game.slots.game.elephantgod.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ElephantGodGameDataDao extends AbstractGameDataDao<ElephantGodPlayerGameDataDTO> {
    public ElephantGodGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(ElephantGodPlayerGameDataDTO.class, mongoTemplate);
    }
}
