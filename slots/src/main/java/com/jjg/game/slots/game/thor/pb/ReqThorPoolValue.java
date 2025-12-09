package com.jjg.game.slots.game.thor.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.thor.ThorConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.THOR, cmd = ThorConstant.MsgBean.REQ_POOL_VALUE)
@ProtoDesc("请求奖池信息")
public class ReqThorPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
