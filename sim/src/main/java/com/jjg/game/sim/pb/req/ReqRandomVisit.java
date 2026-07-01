package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_RANDOM_VISIT)
@ProtoDesc("随机切换拜访玩家")
public class ReqRandomVisit extends AbstractMessage {
    @ProtoDesc("上一个房主id，随机时排除")
    public long lastPlayerId;
}
