package com.jjg.game.slots.game.demonchild.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = DemonChildConstant.MsgBean.REQ_DEMON_CHILD_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqDemonChildPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
