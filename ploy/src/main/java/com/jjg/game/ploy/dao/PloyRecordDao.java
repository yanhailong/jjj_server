package com.jjg.game.ploy.dao;

import com.jjg.game.ploy.data.PloyRecord;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/24
 */
@Repository
public class PloyRecordDao {

    private final int PAGE_SIZE = 20;

    private final MongoTemplate mongoTemplate;

    public PloyRecordDao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 保存记录
     *
     * @param ployRecord
     * @param <T>
     * @return
     */
    public <T extends PloyRecord> T saveRecord(T ployRecord) {
        ployRecord.setTimestamp(System.currentTimeMillis());
        return this.mongoTemplate.save(ployRecord);
    }

    /**
     * 获取最新的记录
     *
     * @param playerId
     * @param roomCfgId
     * @param cla
     * @param <T>
     * @return
     */
    public <T extends PloyRecord> List<T> findLastRecords(long playerId, int roomCfgId, Class<T> cla) {
        return findRecords(playerId, roomCfgId, 0, cla);
    }

    /**
     * 获取历史记录
     *
     * @param playerId
     * @param roomCfgId
     * @param pageIndex
     * @param cla
     * @param <T>
     * @return
     */
    public <T extends PloyRecord> List<T> findRecords(long playerId, int roomCfgId, int pageIndex, Class<T> cla) {
        Query query = new Query();
        query.addCriteria(Criteria.where("playerId").is(playerId));
        query.addCriteria(Criteria.where("roomCfgId").is(roomCfgId));
        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        int skip = pageIndex * PAGE_SIZE;
        query.skip(skip).limit(PAGE_SIZE);
        return this.mongoTemplate.find(query, cla);
    }

    /**
     * 获取总页码
     * @param playerId
     * @param roomCfgId
     * @param cla
     * @return
     * @param <T>
     */
    public <T extends PloyRecord> int allPages(long playerId, int roomCfgId, Class<T> cla) {
        Query query = new Query();
        query.addCriteria(Criteria.where("playerId").is(playerId));
        query.addCriteria(Criteria.where("roomCfgId").is(roomCfgId));

        long allCount = this.mongoTemplate.count(query, cla);

        int allPages = (int)(allCount / PAGE_SIZE);

        if((allCount % PAGE_SIZE) > 0){
            allPages++;
        }
        return allPages;
    }
}
