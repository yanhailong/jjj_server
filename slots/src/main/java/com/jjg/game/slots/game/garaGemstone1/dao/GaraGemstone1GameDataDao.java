package com.jjg.game.slots.game.garaGemstone1.dao;

import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1PlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GaraGemstone1GameDataDao extends AbstractGameDataDao<GaraGemstone1PlayerGameDataDTO> {
    public GaraGemstone1GameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(GaraGemstone1PlayerGameDataDTO.class, mongoTemplate);
    }
}
