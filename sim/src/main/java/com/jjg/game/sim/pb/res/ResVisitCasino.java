package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.VisitCasinoInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_CASINO, resp = true)
@ProtoDesc("拜访赌场返回")
public class ResVisitCasino extends AbstractResponse {
    public VisitCasinoInfo info;
    public int remainingLikes;
    public int dailyLikeLimit;
    public int remainingComments;
    public int dailyCommentLimit;
    public int remainingTrials;
    public int dailyTrialLimit;
    public int unreadComments;
    public boolean commentUnlocked;
    public int commissionRate;

    public ResVisitCasino() {
        super(0);
    }

    public ResVisitCasino(int code) {
        super(code);
    }
}
