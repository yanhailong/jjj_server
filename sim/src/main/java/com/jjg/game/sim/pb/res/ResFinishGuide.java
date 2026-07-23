package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.RES_FINISH_GUIDE, resp = true)
@ProtoDesc("完成新手引导步骤返回")
public class ResFinishGuide extends AbstractResponse {
    @ProtoDesc("完成的引导步骤ID")
    public int guideId;

    public ResFinishGuide(int code) {
        super(code);
    }
}
