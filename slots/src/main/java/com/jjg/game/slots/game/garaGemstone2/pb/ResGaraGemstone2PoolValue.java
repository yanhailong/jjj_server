package com.jjg.game.slots.game.garaGemstone2.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.garaGemstone2.GaraGemstone2Constant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.GARA_GEMSTONE_2, cmd = GaraGemstone2Constant.MsgBean.RES_GARA_GEMSTONE_2_POOL_INFO, resp = true)
@ProtoDesc("返回奖池")
public class ResGaraGemstone2PoolValue extends AbstractResponse {
    public long major;

    public ResGaraGemstone2PoolValue(int code) {
        super(code);
    }
}
