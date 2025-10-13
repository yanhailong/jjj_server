package com.jjg.game.hall.pointsaward.db;

import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 积分大奖数据dao
 */
@Repository
public class PointsAwardDataDao extends MongoBaseDao<PointsAwardData, Long> {

    public PointsAwardDataDao(MongoTemplate mongoTemplate) {
        super(PointsAwardData.class, mongoTemplate);
    }

    /**
     * 获取玩家数据
     *
     * @param playerId 玩家id
     */
    public PointsAwardData findByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.findOne(query, PointsAwardData.class);
    }

}
