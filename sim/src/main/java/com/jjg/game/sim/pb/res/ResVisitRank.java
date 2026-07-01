package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.VisitRankInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_RANK, resp = true)
@ProtoDesc("人气赛季榜返回")
public class ResVisitRank extends AbstractResponse {
    public long seasonEndTime;
    public List<VisitRankInfo> ranks;
    public VisitRankInfo my;

    public ResVisitRank(int code) {
        super(code);
    }
}
