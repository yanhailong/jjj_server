package com.jjg.game.slots.game.wealthgod.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.WealthGodConstant;

/**
 *
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_GOD, cmd = WealthGodConstant.MsgBean.RES_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池结果")
public class ResWealthGodPoolValue extends AbstractResponse {

    public long value;

    public ResWealthGodPoolValue(int code) {
        super(code);
    }
}
