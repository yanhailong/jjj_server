package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.VisitCommentInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_VISIT_COMMENTS, resp = true)
@ProtoDesc("拜访留言板返回")
public class ResVisitComments extends AbstractResponse {
    public int total;
    public List<VisitCommentInfo> comments;
    public int todayPopularity;
    public long totalPopularity;

    public ResVisitComments(int code) {
        super(code);
    }
}
