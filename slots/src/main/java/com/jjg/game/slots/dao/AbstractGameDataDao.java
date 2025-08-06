package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.data.SlotsResultLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
