package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.bountyduel.BountyDuelConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE, cmd = BountyDuelConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("response pool value")
public class ResBountyDuelPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResBountyDuelPoolValue(int code) {
        super(code);
    }
}
