package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author 11
 * @date 2025/8/1 16:53
 */
public abstract class AbstractGameDataDao<T extends SlotsPlayerGameDataDTO> extends MongoBaseDao<T, Long> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    public AbstractGameDataDao(Class<T> clazz, MongoTemplate mongoTemplate) {
        super(clazz, mongoTemplate);
    }

    public T getGameDataByPlayerId(long playerId,int roomCfgId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("roomCfgId").is(roomCfgId));
        return mongoTemplate.findOne(query, clazz);
    }

    public T saveGameData(T playerGameData) {
        return mongoTemplate.save(playerGameData);
    }
}
