package com.jjg.game.slots.game.candyparty.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CANDY_PARTY, cmd = CandyPartyConstant.MsgBean.REQ_CANDY_PARTY_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqCandyPartyPoolValue extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeValue;
}
