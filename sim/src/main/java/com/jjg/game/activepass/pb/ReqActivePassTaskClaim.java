package com.jjg.game.activepass.pb;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_ACTIVE_PASS_TASK_CLAIM)
@ProtoDesc("手动领取活跃通行证任务积分")
public class ReqActivePassTaskClaim extends AbstractMessage {
    public int passId;
    public int taskId;
    /** 每日任务需回传列表中的day，避免跨日旧请求领取新任务；周期任务可传0。 */
    public int day;
}
