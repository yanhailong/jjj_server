package com.jjg.game.slots.game.pegasusunbridle.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;


@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = PegasusUnbridleConstant.MsgBean.REQ_PEGASUS_UNBRIDLE_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqPegasusUnbridlePoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
