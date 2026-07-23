package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.NOTIFY_GUIDE_TRIGGER, resp = true)
@ProtoDesc("触发新手引导组通知")
public class NotifyGuideTrigger extends AbstractResponse {
    @ProtoDesc("本次触发的引导组编号列表")
    public List<Integer> guideGroupIds;

    public NotifyGuideTrigger(int code) {
        super(code);
    }
}
