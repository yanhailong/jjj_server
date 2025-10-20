package com.jjg.game.core.dao;

import com.jjg.game.core.data.AwardCode;
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
