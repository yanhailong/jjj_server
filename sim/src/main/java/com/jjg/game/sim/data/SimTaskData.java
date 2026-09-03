package com.jjg.game.sim.data;

import com.jjg.game.core.task.db.TaskDetail;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 玩家 sim 任务数据 (主线 + 成就)。
 * <p>
 * 主线保存线性链当前节点；成就按 group 保存各条线性链的当前节点。
 * 复用 core {@link TaskDetail} 作进度单元。
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
     * 成就任务: group -> 当前节点，末节点领取后保留，表示该组已全部完成。
     */
    private Map<Integer, TaskDetail> achievementTasks = new HashMap<>();

    /**
     * 场景与个人简介共用的成就徽章ID(MedalBuff.MedalType)，顺序即展示顺序。
     */
    private List<Integer> displayedMedalIds = new ArrayList<>();


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

    public Map<Integer, TaskDetail> getAchievementTasks() {
        if (achievementTasks == null) {
            achievementTasks = new HashMap<>();
        }
        return achievementTasks;
    }

    public void setAchievementTasks(Map<Integer, TaskDetail> achievementTasks) {
        this.achievementTasks = achievementTasks == null ? new HashMap<>() : achievementTasks;
    }

    public List<Integer> getDisplayedMedalIds() {
        if (displayedMedalIds == null) {
            displayedMedalIds = new ArrayList<>();
        }
        return displayedMedalIds;
    }

    public void setDisplayedMedalIds(List<Integer> displayedMedalIds) {
        this.displayedMedalIds = displayedMedalIds == null ? new ArrayList<>() : displayedMedalIds;
    }

}
