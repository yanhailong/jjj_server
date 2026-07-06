package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/**
 * 发起者请求领取多人任务奖励。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_COOP_TASK_REWARD)
@ProtoDesc("请求领取多人任务奖励")
public class ReqCoopTaskReward extends AbstractMessage {
    @ProtoDesc("任务配置id")
    public int taskId;
}
