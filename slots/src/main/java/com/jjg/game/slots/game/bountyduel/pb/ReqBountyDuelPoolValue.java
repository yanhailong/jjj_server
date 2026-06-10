package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE, cmd = BountyDuelConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("request pool value")
public class ReqBountyDuelPoolValue extends AbstractMessage {
    @ProtoDesc("stake value")
    public long stakeVlue;
}
