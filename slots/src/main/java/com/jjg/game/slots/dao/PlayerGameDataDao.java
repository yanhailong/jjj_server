package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;

/**
 * @author 11
 * @date 2025/8/1 16:53
 */
@Repository
public class PlayerGameDataDao extends MongoBaseDao<SlotsPlayerGameData, Long> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    public PlayerGameDataDao(MongoTemplate mongoTemplate) {
        super(SlotsPlayerGameData.class, mongoTemplate);
    }

    public SlotsPlayerGameData getPlayerGameDataByPlayerId(long playerId, int roomCfgId, long roomId, Class<? extends SlotsPlayerGameData> tClass) {
        String id = playerId + ":" + roomCfgId + ":" + roomId;
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.findOne(query, tClass, getRoomCollectionName(tClass));
    }

    @SuppressWarnings("unchecked")
    public SlotsPlayerGameData savePlayerGameData(SlotsPlayerGameData playerGameData) {
        if (playerGameData.getId() == null || playerGameData.getId().isEmpty()) {
            playerGameData.buildRoomKey();
        }
        Query query = Query.query(Criteria.where("_id").is(playerGameData.getId()));
        Update update = buildUpdate(playerGameData);
        Class<SlotsPlayerGameData> tClass = (Class<SlotsPlayerGameData>) playerGameData.getClass();
        update.set("_class", tClass.getName());
        return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true),
                tClass, getRoomCollectionName(tClass));
    }

    public long deletePlayerGameDataRoomOnDisband(long roomId, Class<? extends SlotsPlayerGameData> tClass) {
        Query query = Query.query(Criteria.where("roomId").is(roomId));
        return mongoTemplate.remove(query, tClass, getRoomCollectionName(tClass)).getDeletedCount();
    }

    protected String getRoomCollectionName(Class<? extends SlotsPlayerGameData> tClass) {
        return tClass.getSimpleName();
    }

    protected Update buildUpdate(SlotsPlayerGameData playerGameData) {
        Update update = new Update();
        // 设置所有需要更新的字段
        ReflectionUtils.doWithFields(playerGameData.getClass(), field -> {
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers()) && !field.getName().equals("id") && !Modifier.isTransient(field.getModifiers())) {
                update.set(field.getName(), field.get(playerGameData));
            }
        });
        return update;
    }
}
