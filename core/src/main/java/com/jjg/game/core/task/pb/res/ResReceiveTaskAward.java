package com.jjg.game.core.task.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.TaskConstant;

/**
 * 请求领取任务奖励回复
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TASK_TYPE, cmd = TaskConstant.MsgBean.RES_TASK_REWARD, resp = true)
@ProtoDesc("请求领取任务奖励回复")
public class ResReceiveTaskAward extends AbstractResponse {

    public ResReceiveTaskAward(int code) {
        super(code);
    }
}
