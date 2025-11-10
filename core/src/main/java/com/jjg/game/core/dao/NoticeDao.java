package com.jjg.game.core.dao;

import com.jjg.game.core.data.Notice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2025/11/10 10:31
 */
@Repository
public class NoticeDao extends MongoBaseDao<Notice, Long>{
    public NoticeDao(MongoTemplate mongoTemplate) {
        super(Notice.class, mongoTemplate);
    }

    public long delNotice(List<Long> ids){
        return mongoTemplate.remove(new Query(Criteria.where("_id").in(ids)), clazz).getDeletedCount();
    }

    public List<Notice> getNoticeList(int time){
        Query query = new Query(Criteria.where("open").is(true));
        query.addCriteria(Criteria.where("startTime").lt(time));
        query.addCriteria(Criteria.where("endTime").gt(time));
        return mongoTemplate.find(query, clazz);
    }
}
