package com.jjg.game.slots.game.moneyrabbit.dao;

import com.jjg.game.slots.dao.AbstractGameDataDao;
import com.jjg.game.slots.game.moneyrabbit.data.MoneyRabbitPlayerGameDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MoneyRabbitGameDataDao extends AbstractGameDataDao<MoneyRabbitPlayerGameDataDTO> {
    public MoneyRabbitGameDataDao(@Autowired MongoTemplate mongoTemplate) {
        super(MoneyRabbitPlayerGameDataDTO.class, mongoTemplate);
    }
}
