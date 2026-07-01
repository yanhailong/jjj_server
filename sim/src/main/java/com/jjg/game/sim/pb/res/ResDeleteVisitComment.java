package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_DELETE_VISIT_COMMENT, resp = true)
@ProtoDesc("删除拜访留言返回")
public class ResDeleteVisitComment extends AbstractResponse {
    public String commentId;

    public ResDeleteVisitComment(int code) {
        super(code);
    }
}
