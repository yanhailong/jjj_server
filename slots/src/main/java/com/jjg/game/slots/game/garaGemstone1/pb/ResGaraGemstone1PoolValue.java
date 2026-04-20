package com.jjg.game.slots.game.garaGemstone1.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone1.GaraGemstone1Constant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_1, cmd = GaraGemstone1Constant.MsgBean.RES_LUCKY_MOUSE_POOL_INFO, resp = true)
@ProtoDesc("返回奖池")
public class ResGaraGemstone1PoolValue extends AbstractResponse {
    public long major;

    public ResGaraGemstone1PoolValue(int code) {
        super(code);
    }
}
