package com.jjg.game.slots.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;

/**
 * @author 11
 * @date 2025/8/1 16:53
 */
public abstract class AbstractGameDataDao<T extends SlotsPlayerGameDataDTO> extends MongoBaseDao<T, Long> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    public AbstractGameDataDao(Class<T> clazz, MongoTemplate mongoTemplate) {
        super(clazz, mongoTemplate);
    }

    public T getGameDataByPlayerId(long playerId, int roomCfgId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("roomCfgId").is(roomCfgId));
        return mongoTemplate.findOne(query, clazz);
    }

    public T saveGameData(T playerGameData) {
        Query query = Query.query(Criteria.where("playerId").is(playerGameData.getPlayerId()).and("roomCfgId").is(playerGameData.getRoomCfgId()));

        Update update = new Update();
        // 设置所有需要更新的字段
        ReflectionUtils.doWithFields(clazz, field -> {
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers()) && !field.getName().equals("id")) {
                update.set(field.getName(), field.get(playerGameData));
            }
        });

        update.set("_class", playerGameData.getClass().getName());

        // 设置upsert为true，如果不存在就插入
        return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), clazz);
    }
}
