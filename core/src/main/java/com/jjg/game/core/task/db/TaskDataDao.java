package com.jjg.game.core.task.db;

import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;


/**
 * 任务数据dao
 */
@Repository
public class TaskDataDao extends MongoBaseDao<TaskData, Long> {

    public TaskDataDao(MongoTemplate mongoTemplate) {
        super(TaskData.class, mongoTemplate);
    }

    /**
     * 根据玩家id获取玩家所有任务
     *
     * @param playerId 玩家id
     * @return
     */
    public TaskData findByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.findOne(query, TaskData.class);
    }
}
