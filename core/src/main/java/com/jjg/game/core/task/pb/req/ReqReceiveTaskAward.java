package com.jjg.game.core.task.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.TaskConstant;

/**
 * 请求获取任务奖励
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TASK_TYPE, cmd = TaskConstant.MsgBean.REQ_TASK_REWARD)
@ProtoDesc("请求获取任务奖励")
public class ReqReceiveTaskAward extends AbstractMessage {

    /**
     * 任务配置id
     */
    @ProtoDesc("任务配置id")
    private int taskId;

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }
}
