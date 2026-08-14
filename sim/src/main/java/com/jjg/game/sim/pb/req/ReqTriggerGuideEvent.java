package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/** 客户端主动上报新手引导触发事件。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.REQ_TRIGGER_GUIDE_EVENT)
@ProtoDesc("客户端主动上报新手引导触发事件")
public class ReqTriggerGuideEvent extends AbstractMessage {
    @ProtoDesc("触发类型，当前只允许传10")
    public int condition;
    @ProtoDesc("触发参数，例如进入斗仙牌节点传10000")
    public int param;
}
