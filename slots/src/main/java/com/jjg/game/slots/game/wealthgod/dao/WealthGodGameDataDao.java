package com.jjg.game.slots.game.wealthgod.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.wealthgod.data.WealthGodPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 财神
 */
@Repository
public class WealthGodGameDataDao extends AbstractGameDataDao<WealthGodPlayerGameDataDTO> {

    public WealthGodGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(WealthGodPlayerGameDataDTO.class, mongoTemplate);
    }

}