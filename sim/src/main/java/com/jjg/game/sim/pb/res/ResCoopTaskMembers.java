package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.SimCoopMemberInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_COOP_TASK_MEMBERS, resp = true)
@ProtoDesc("多人任务当前人数返回")
public class ResCoopTaskMembers extends AbstractResponse {
    @ProtoDesc("玩家任务当前人数列表")
    public List<SimCoopMemberInfo> members;

    public ResCoopTaskMembers(int code) {
        super(code);
    }
}
