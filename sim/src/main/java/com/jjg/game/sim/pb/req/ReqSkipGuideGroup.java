package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

/** 请求跳过整个新手引导组。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.REQ_SKIP_GUIDE_GROUP)
@ProtoDesc("请求跳过整个新手引导组")
public class ReqSkipGuideGroup extends AbstractMessage {
    @ProtoDesc("需要跳过的引导组ID")
    public int guideGroupId;
}
