package com.jjg.game.slots.game.captainjack.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;
import com.jjg.game.slots.game.thor.ThorConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = CaptainJackConstant.MsgBean.RES_CAPTAIN_JACK_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResCaptainJackPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResCaptainJackPoolValue(int code) {
        super(code);
    }
}
