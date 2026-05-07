package com.jjg.game.slots.game.garaGemstone3.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone3.GaraGemstone3Constant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_3, cmd = GaraGemstone3Constant.MsgBean.RES_GARA_GEMSTONE_3_POOL_INFO, resp = true)
@ProtoDesc("返回奖池")
public class ResGaraGemstone3PoolValue extends AbstractResponse {
    public long major;

    public ResGaraGemstone3PoolValue(int code) {
        super(code);
    }
}
