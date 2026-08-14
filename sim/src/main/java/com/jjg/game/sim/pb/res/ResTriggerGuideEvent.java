package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/** 客户端新手引导触发事件处理结果。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.RES_TRIGGER_GUIDE_EVENT, resp = true)
@ProtoDesc("客户端新手引导触发事件处理结果")
public class ResTriggerGuideEvent extends AbstractResponse {
    @ProtoDesc("本次上报的触发类型")
    public int condition;
    @ProtoDesc("本次上报的触发参数")
    public int param;

    public ResTriggerGuideEvent(int code) {
        super(code);
    }
}
