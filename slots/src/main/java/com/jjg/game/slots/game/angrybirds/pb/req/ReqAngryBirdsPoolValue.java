package com.jjg.game.slots.game.angrybirds.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.angrybirds.constant.AngryBirdsConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ANGRY_BIRDS, cmd = AngryBirdsConstant.MsgBean.REQ_ANGRY_BIRDS_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqAngryBirdsPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
