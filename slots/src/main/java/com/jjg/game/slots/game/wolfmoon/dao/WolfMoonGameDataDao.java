package com.jjg.game.slots.game.wolfmoon.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WolfMoonGameDataDao extends AbstractGameDataDao<WolfMoonPlayerGameDataDTO> {
    public WolfMoonGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(WolfMoonPlayerGameDataDTO.class, mongoTemplate);
    }
}
