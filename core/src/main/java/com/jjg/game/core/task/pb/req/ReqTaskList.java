package com.jjg.game.core.task.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.TaskConstant;

/**
 * 获取任务列表
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TASK_TYPE, cmd = TaskConstant.MsgBean.REQ_TASK)
@ProtoDesc("获取任务列表")
public class ReqTaskList extends AbstractMessage {

    /**
     * 任务类型 1=积分任务
     */
    @ProtoDesc("任务类型 1=积分任务")
    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
