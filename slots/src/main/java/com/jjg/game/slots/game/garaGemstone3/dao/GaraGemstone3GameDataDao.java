package com.jjg.game.slots.game.garaGemstone3.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3PlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GaraGemstone3GameDataDao extends AbstractGameDataDao<GaraGemstone3PlayerGameDataDTO> {
    public GaraGemstone3GameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(GaraGemstone3PlayerGameDataDTO.class, mongoTemplate);
    }
}
