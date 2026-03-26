package com.jjg.game.ploy.dao;

import com.jjg.game.ploy.data.PlayerPloyGameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2026/3/19
 */
@Repository
public class PlayerPloyGameDataDao {
    protected Logger log = LoggerFactory.getLogger(getClass());

    private final MongoTemplate mongoTemplate;

    public PlayerPloyGameDataDao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 保存数据
     *
     * @param playerGameData
     * @param <T>
     * @return
     */
    public <T extends PlayerPloyGameData> T saveGameData(T playerGameData) {
        return this.mongoTemplate.save(playerGameData);
    }

    /**
     * 获取一个
     *
     * @param playerId
     * @param roomCfgId
     * @param cla
     * @param <T>
     * @return
     */
    public <T extends PlayerPloyGameData> T findOne(long playerId, long roomCfgId, Class<T> cla) {
        Query query = new Query();
        query.addCriteria(Criteria.where("playerId").is(playerId));
        query.addCriteria(Criteria.where("roomCfgId").is(roomCfgId));
        return this.mongoTemplate.findOne(query, cla);
    }
}
