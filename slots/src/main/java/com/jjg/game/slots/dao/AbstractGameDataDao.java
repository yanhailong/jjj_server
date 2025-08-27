package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

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
        return mongoTemplate.findById(playerId, clazz, tableName(roomCfgId));
    }

    public T saveGameData(T playerGameData) {
        return mongoTemplate.save(playerGameData, tableName(playerGameData.getRoomCfgId()));
    }

    private String tableName(int roomCfgId) {
        return clazz.getSimpleName() + "_" + roomCfgId;
    }
}
