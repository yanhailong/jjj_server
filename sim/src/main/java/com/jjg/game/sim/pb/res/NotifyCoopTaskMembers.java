package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.SimCoopMemberInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.NOTIFY_COOP_TASK_MEMBERS, resp = true)
@ProtoDesc("多人任务人员数量变更通知")
public class NotifyCoopTaskMembers extends AbstractResponse {
    @ProtoDesc("协作房间id")
    public long roomId;
    @ProtoDesc("多人任务人数信息")
    public SimCoopMemberInfo member;

    public NotifyCoopTaskMembers(int code) {
        super(code);
    }
}
