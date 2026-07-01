package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_START_VISIT_TRIAL)
@ProtoDesc("开始客座赌局试玩")
public class ReqStartVisitTrial extends AbstractMessage {
    public long playerId;
    public int casinoId;
    public int gameType;
}
