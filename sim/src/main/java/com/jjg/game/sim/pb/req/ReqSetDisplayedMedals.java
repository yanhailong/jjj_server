package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_SET_DISPLAYED_MEDALS)
@ProtoDesc("设置经营信息展示的成就勋章")
public class ReqSetDisplayedMedals extends AbstractMessage {
    @ProtoDesc("要展示的勋章配置id")
    public List<Integer> medalIds;
}
