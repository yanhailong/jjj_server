package com.jjg.game.slots.game.dollarexpress.dao;

import com.jjg.game.core.dao.MongoBaseDao;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/6/23 10:54
 */
@Repository
public class DollarExpressResultDao extends MongoBaseDao<DollarExpressResult, Long> {
    public DollarExpressResultDao(@Autowired MongoTemplate mongoTemplate) {
        super(DollarExpressResult.class, mongoTemplate);
    }

    public long getResultCount(DollarExpressResult dollarExpressResult) {
        //获取条数，非精确
        return this.mongoTemplate.estimatedCount(getClass());
    }

    public void removeTable(){
        this.mongoTemplate.dropCollection(getClass());
    }
}
