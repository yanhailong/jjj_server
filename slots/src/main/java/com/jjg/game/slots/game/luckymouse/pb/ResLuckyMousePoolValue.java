package com.jjg.game.slots.game.luckymouse.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.luckymouse.LuckyMouseConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE, cmd = LuckyMouseConstant.MsgBean.RES_LUCKY_MOUSE_POOL_INFO, resp = true)
@ProtoDesc("返回奖池")
public class ResLuckyMousePoolValue extends AbstractResponse {
    public long major;

    public ResLuckyMousePoolValue(int code) {
        super(code);
    }
}
