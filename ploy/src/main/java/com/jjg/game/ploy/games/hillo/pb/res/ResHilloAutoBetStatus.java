package com.jjg.game.ploy.games.hillo.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.ploy.games.hillo.data.HilloConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HILLO, cmd = HilloConstant.MsgBean.RES_HILLO_AUTO_BET_STATUS, resp = true)
@ProtoDesc("HILLO 自动投注状态")
public class ResHilloAutoBetStatus extends AbstractResponse {
    @ProtoDesc("是否正在自动投注")
    public boolean autoBetting;

    public ResHilloAutoBetStatus(int code) {
        super(code);
    }
}
