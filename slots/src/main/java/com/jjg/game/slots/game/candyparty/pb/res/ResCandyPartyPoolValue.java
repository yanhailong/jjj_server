package com.jjg.game.slots.game.candyparty.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.candyparty.constant.CandyPartyConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CANDY_PARTY, cmd = CandyPartyConstant.MsgBean.RES_CANDY_PARTY_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResCandyPartyPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResCandyPartyPoolValue(int code) {
        super(code);
    }
}
