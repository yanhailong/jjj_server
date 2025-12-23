package com.jjg.game.core.task.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务数据
 */
@Document
public class TaskData {
    /**
     * 玩家id
     */
    @Id
    private long playerId;

    /**
     * 任务详情
     */
    private Map<Integer, TaskDetail> taskDetails = new ConcurrentHashMap<>();


    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, TaskDetail> getTaskDetails() {
        return taskDetails;
    }

    public void setTaskDetails(Map<Integer, TaskDetail> taskDetails) {
        this.taskDetails = taskDetails;
    }

    public void addTaskDetail(TaskDetail taskDetail) {
        if (taskDetails == null) {
            taskDetails = new ConcurrentHashMap<>();
        }
        taskDetails.put(taskDetail.getConfigId(), taskDetail);
    }

    public TaskDetail getTaskDetail(int configId) {
        if (taskDetails == null) {
            return null;
        }
        return taskDetails.get(configId);
    }

    public boolean hasTask(int configId){
        if (taskDetails == null) {
            return false;
        }
        return taskDetails.containsKey(configId);
    }
}
