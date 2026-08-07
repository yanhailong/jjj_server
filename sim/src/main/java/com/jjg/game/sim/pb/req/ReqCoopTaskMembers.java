package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.SimCoopMemberInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_COOP_TASK_MEMBERS)
@ProtoDesc("请求多人任务当前人数")
public class ReqCoopTaskMembers extends AbstractMessage {
    @ProtoDesc("玩家任务列表")
    public List<SimCoopMemberInfo> members;
}
