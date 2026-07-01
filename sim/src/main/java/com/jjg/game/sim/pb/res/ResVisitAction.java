package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_ACTION, resp = true)
@ProtoDesc("拜访基础操作返回")
public class ResVisitAction extends AbstractResponse {
    public int reason;
    public int addedPopularity;
    public int todayPopularity;
    public long totalPopularity;
    public int remainingCount;
    public long diamond;

    public ResVisitAction(int code) {
        super(code);
    }
}
