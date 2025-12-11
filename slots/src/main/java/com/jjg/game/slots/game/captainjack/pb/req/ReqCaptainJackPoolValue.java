package com.jjg.game.slots.game.captainjack.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.captainjack.constant.CaptainJackConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = CaptainJackConstant.MsgBean.REQ_CAPTAIN_JACK_POOL_VALUE)
@ProtoDesc("请求奖池")
public class ReqCaptainJackPoolValue extends AbstractResponse {
    @ProtoDesc("下注金额")
    public long stakeValue;
    public ReqCaptainJackPoolValue(int code) {
        super(code);
    }
}
