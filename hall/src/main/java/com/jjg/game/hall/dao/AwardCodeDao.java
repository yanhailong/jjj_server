package com.jjg.game.hall.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.hall.data.AwardCode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 领奖码dao
 */
@Repository
public class AwardCodeDao extends MongoBaseDao<AwardCode, Long> {

    public AwardCodeDao(MongoTemplate mongoTemplate) {
        super(AwardCode.class, mongoTemplate);
    }

}
