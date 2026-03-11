package com.jjg.game.core.dao;

import com.jjg.game.core.data.Notice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2025/11/10 10:31
 */
@Repository
public class NoticeDao extends MongoBaseDao<Notice, Long> {

    public final String TABLE_NAME = "notice:";

    @Autowired
    private RedisTemplate redisTemplate;

    public NoticeDao(MongoTemplate mongoTemplate) {
        super(Notice.class, mongoTemplate);
    }

    private String getTableName(long playerId) {
        return TABLE_NAME + playerId;
    }

    public long delNotice(List<Long> ids) {
        return mongoTemplate.remove(new Query(Criteria.where("_id").in(ids)), clazz).getDeletedCount();
    }

    public List<Notice> getNoticeList(int time) {
        Query query = new Query(Criteria.where("open").is(true));
        query.addCriteria(Criteria.where("startTime").lt(time));
        query.addCriteria(Criteria.where("endTime").gt(time));
        return mongoTemplate.find(query, clazz);
    }

    /**
     * 阅读邮件
     *
     * @param playerId
     * @param noticeId
     */
    public void readNotice(long playerId, long noticeId) {
        redisTemplate.opsForSet().add(getTableName(playerId), noticeId);
    }

    public void removeReadData(long playerId) {
        redisTemplate.delete(getTableName(playerId));
    }

    public Set<Long> getPlayerReadNotice(long playerId) {
        Set members = redisTemplate.opsForSet().members(getTableName(playerId));
        if(members == null){
            return Collections.emptySet();
        }

        Set<Long> set = new HashSet<>(members.size());
        members.forEach(m -> set.add(Long.parseLong(m.toString())));
        return set;
    }
}
