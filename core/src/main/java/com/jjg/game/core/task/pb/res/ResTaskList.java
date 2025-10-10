package com.jjg.game.core.task.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.task.pb.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取任务列表回复
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TASK_TYPE, cmd = TaskConstant.MsgBean.RES_TASK, resp = true)
@ProtoDesc("获取任务列表回复")
public class ResTaskList extends AbstractResponse {

    /**
     * 任务类型 1=积分任务
     */
    @ProtoDesc("任务类型 1=积分任务")
    private int type;

    /**
     * 任务列表
     */
    @ProtoDesc("任务列表")
    private List<Task> taskList = new ArrayList<>();

    public ResTaskList(int code) {
        super(code);
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
