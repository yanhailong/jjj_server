package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_TRIAL, resp = true)
@ProtoDesc("客座赌局会话返回")
public class ResVisitTrial extends AbstractResponse {
    public String sessionId;
    public long playerId;
    public int casinoId;
    public int gameType;
    public int remainingTrials;
    public int power;
    public long expireTime;

    public ResVisitTrial(int code) {
        super(code);
    }
}
