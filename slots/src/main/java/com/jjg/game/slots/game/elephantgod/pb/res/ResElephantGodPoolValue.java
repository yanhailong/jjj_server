package com.jjg.game.slots.game.elephantgod.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.elephantgod.ElephantGodConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ELEPHANT_GOD, cmd = ElephantGodConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池")
public class ResElephantGodPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResElephantGodPoolValue(int code) {
        super(code);
    }
}
