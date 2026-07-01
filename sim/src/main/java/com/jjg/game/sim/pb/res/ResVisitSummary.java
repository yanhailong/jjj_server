package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_SUMMARY, resp = true)
@ProtoDesc("拜访当日汇总返回")
public class ResVisitSummary extends AbstractResponse {
    public long visitorCount;
    public long commissionGold;
    public int todayPopularity;
    public long totalPopularity;

    public ResVisitSummary(int code) {
        super(code);
    }
}
