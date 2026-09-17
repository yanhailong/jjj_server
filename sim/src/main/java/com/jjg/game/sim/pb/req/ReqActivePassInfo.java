package com.jjg.game.sim.pb.req;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_ACTIVE_PASS_INFO)
@ProtoDesc("获取活跃通行证")
public class ReqActivePassInfo extends AbstractMessage {
}
