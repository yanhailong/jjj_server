package com.jjg.game.slots.game.garaGemstone2.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.garaGemstone2.data.GaraGemstone2PlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GaraGemstone2GameDataDao extends AbstractGameDataDao<GaraGemstone2PlayerGameDataDTO> {
    public GaraGemstone2GameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(GaraGemstone2PlayerGameDataDTO.class, mongoTemplate);
    }
}
