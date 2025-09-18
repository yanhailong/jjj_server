package com.jjg.game.hall.minigame.game.luckytreasure.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.minigame.game.luckytreasure.data.LuckyTreasure;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 夺宝奇兵数据dao
 */
@Repository
public class LuckyTreasureDao extends MongoBaseDao<LuckyTreasure, Long> {

    public LuckyTreasureDao(MongoTemplate mongoTemplate) {
        super(LuckyTreasure.class, mongoTemplate);
    }

    /**
     * 根据endTime查询记录
     *
     * @param endTime 结束时间
     * @param limit   最大查询数量，0表示不限制
     * @return 指定endTime的LuckyTreasure列表
     */
    public List<LuckyTreasure> findAllByEndTime(long endTime, int limit) {
        Query query = new Query(Criteria.where("endTime").is(endTime));
        if (limit > 0) {
            query.limit(limit);
        }
        return mongoTemplate.find(query, LuckyTreasure.class);
    }

}
