package com.jjg.game.sim.data;

import com.jjg.game.core.task.db.TaskDetail;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家 sim 任务数据 (主线 + 成就)。
 * <p>
 * 主线/成就都是线性链, 每条链内存仅保留"当前节点": 已领取的前置节点客户端依配置链自行渲染,
 * 未解锁的后置节点同理, 服务端只需保存当前推进到的节点进度即可。复用 core {@link TaskDetail} 作进度单元。
 *
 * @author 11
 * @date 2026/6/25
 */
@Document
public class SimTaskData extends AbstractData {
    @Id
    private long playerId;

    /**
     * 主线当前节点 (整条主线唯一一条进行中/已完成的任务)
     */
    private TaskDetail mainTask;

    /**
     * 成就当前节点: group -> 该成就组当前阶梯节点
     */
    private Map<Integer, TaskDetail> achievements = new HashMap<>();

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public TaskDetail getMainTask() {
        return mainTask;
    }

    public void setMainTask(TaskDetail mainTask) {
        this.mainTask = mainTask;
    }

    public Map<Integer, TaskDetail> getAchievements() {
        if (achievements == null) {
            achievements = new HashMap<>();
        }
        return achievements;
    }

    public void setAchievements(Map<Integer, TaskDetail> achievements) {
        this.achievements = achievements == null ? new HashMap<>() : achievements;
    }
}
