package com.jjg.game.slots.game.goldsnakefortune.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortunePlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GoldSnakeFortuneGameDataDao extends AbstractGameDataDao<GoldSnakeFortunePlayerGameDataDTO> {
    public GoldSnakeFortuneGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(GoldSnakeFortunePlayerGameDataDTO.class, mongoTemplate);
    }
}
