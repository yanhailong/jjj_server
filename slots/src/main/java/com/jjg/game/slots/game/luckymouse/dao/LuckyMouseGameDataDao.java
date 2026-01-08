package com.jjg.game.slots.game.luckymouse.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.luckymouse.data.LuckyMousePlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LuckyMouseGameDataDao extends AbstractGameDataDao<LuckyMousePlayerGameDataDTO> {
    public LuckyMouseGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(LuckyMousePlayerGameDataDTO.class, mongoTemplate);
    }
}
