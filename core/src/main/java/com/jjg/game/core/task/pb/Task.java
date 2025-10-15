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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
