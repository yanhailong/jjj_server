package com.jjg.game.core.task.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务
 */
@ProtobufMessage
@ProtoDesc("任务")
public class Task {

    /**
     * 任务配置id
     */
    @ProtoDesc("任务配置id")
    private int configId;

    /**
     * 任务条件
     */
    @ProtoDesc("任务条件")
    private List<TaskCondition> conditions = new ArrayList<>();

    /**
     * 任务奖励
     */
    @ProtoDesc("任务奖励")
    private List<TaskReward> rewards = new ArrayList<>();

    /**
     * 按钮跳转
     */
    @ProtoDesc("按钮跳转")
    private List<TaskJump> jumps = new ArrayList<>();

    /**
     * 任务图标
     */
    @ProtoDesc("任务图标")
    private String taskIcon;

    /**
     * 任务状态 0-进行中 1-已完成 2-已领取奖励
     */
    @ProtoDesc("任务状态 0-进行中 1-已完成 2-已领取奖励")
    private int status;

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public List<TaskCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<TaskCondition> conditions) {
        this.conditions = conditions;
    }

    public List<TaskReward> getRewards() {
        return rewards;
    }

    public void setRewards(List<TaskReward> rewards) {
        this.rewards = rewards;
    }

    public List<TaskJump> getJumps() {
        return jumps;
    }

    public void setJumps(List<TaskJump> jumps) {
        this.jumps = jumps;
    }

    public String getTaskIcon() {
        return taskIcon;
    }

    public void setTaskIcon(String taskIcon) {
        this.taskIcon = taskIcon;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
