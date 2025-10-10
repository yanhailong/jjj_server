package com.jjg.game.core.task.db;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 任务数据
 */
@Document(collection = "TaskData")
public class TaskData {

    /**
     * 配置id
     */
    private int configId;

    /**
     * 玩家id
     */
    private long playerId;

    /**
     * 任务状态
     */
    private int status;

    /**
     * 任务进度
     * k=条件id
     * v=进度
     */
    private Map<Integer, Long> progress = new HashMap<>();

    /**
     * 表示任务完成条件的集合。
     * finishConditions集合存储的是任务的完成条件ID，每个ID表示一种具体的任务完成条件。
     * 当玩家满足指定条件时，任务将被标记为完成。
     */
    private Set<Integer> finishConditionIds = new HashSet<>();

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 完成时间
     */
    private long completeTime;

    /**
     * 奖励领取时间
     */
    private long rewardTime;

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<Integer, Long> getProgress() {
        return progress;
    }

    public void setProgress(Map<Integer, Long> progress) {
        this.progress = progress;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    public long getRewardTime() {
        return rewardTime;
    }

    public void setRewardTime(long rewardTime) {
        this.rewardTime = rewardTime;
    }

    public Set<Integer> getFinishConditionIds() {
        return finishConditionIds;
    }

    public void setFinishConditionIds(Set<Integer> finishConditionIds) {
        this.finishConditionIds = finishConditionIds;
    }
}
