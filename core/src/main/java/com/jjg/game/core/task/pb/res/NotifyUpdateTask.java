package com.jjg.game.core.task.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.task.pb.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知任务进度更新
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TASK_TYPE, cmd = TaskConstant.MsgBean.NOTIFY_TASK_UPDATE, resp = true)
@ProtoDesc("通知任务进度更新")
public class NotifyUpdateTask extends AbstractNotice {

    /**
     * 更新的任务列表
     */
    @ProtoDesc("更新的任务列表")
    private List<Task> taskList = new ArrayList<>();

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }
}
