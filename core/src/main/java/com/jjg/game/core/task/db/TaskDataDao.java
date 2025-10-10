package com.jjg.game.core.task.db;

import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.MongoBaseDao;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

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
    public List<TaskData> findByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        return mongoTemplate.find(query, TaskData.class);
    }

    /**
     * 根据玩家id获取玩家所有任务
     *
     * @param playerId 玩家id
     * @return
     */
    public List<TaskData> findByPlayerId(long playerId, int status) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("status").is(status));
        return mongoTemplate.find(query, TaskData.class);
    }

    /**
     * 根据玩家ID和任务ID查询特定任务
     *
     * @param playerId 玩家ID
     * @param configId 任务ID
     * @return 玩家的特定任务
     */
    public TaskData findByPlayerIdAndConfigId(long playerId, int configId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("configId").is(configId));
        return mongoTemplate.findOne(query, TaskData.class);
    }

    /**
     * 根据玩家ID和任务状态查询任务
     *
     * @param playerId 玩家ID
     * @param status   任务状态
     * @return 符合条件的任务列表
     */
    public List<TaskData> findByPlayerIdAndStatus(long playerId, int status) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("status").is(status));
        return mongoTemplate.find(query, TaskData.class);
    }

    /**
     * 查询玩家的可领取奖励任务
     *
     * @param playerId 玩家ID
     * @return 可领取奖励的任务列表
     */
    public List<TaskData> findRewardedTasks(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId)
                .and("status").is(TaskConstant.TaskStatus.STATUS_COMPLETED));
        return mongoTemplate.find(query, TaskData.class);
    }

    /**
     * 根据玩家ID删除所有任务
     *
     * @param playerId 玩家ID
     */
    public void deleteByPlayerId(long playerId) {
        Query query = new Query(Criteria.where("playerId").is(playerId));
        mongoTemplate.remove(query, TaskData.class);
    }

    /**
     * 根据玩家ID删除所有任务
     *
     * @param playerId 玩家ID
     */
    public void deleteByPlayerId(long playerId, int taskId) {
        Query query = new Query(Criteria.where("playerId").is(playerId).and("configId").is(taskId));
        mongoTemplate.remove(query, TaskData.class);
    }

    /**
     * 批量删除任务
     *
     * @param dataList 要删除的任务列表
     */
    public void deleteAllList(List<TaskData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        List<Criteria> criteriaList = new ArrayList<>();
        for (TaskData data : dataList) {
            criteriaList.add(Criteria.where("playerId").is(data.getPlayerId())
                    .and("configId").is(data.getConfigId()));
        }
        Query query = new Query(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        mongoTemplate.remove(query, TaskData.class);
    }

}
