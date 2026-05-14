package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_EXCHANGE, resp = true)
@ProtoDesc("HILLO exchange result")
public class ResHilloExchange extends AbstractResponse {
    @ProtoDesc("get gold num")
    public long getGoldNum;

    public ResHilloExchange(int code) {
        super(code);
    }
}
