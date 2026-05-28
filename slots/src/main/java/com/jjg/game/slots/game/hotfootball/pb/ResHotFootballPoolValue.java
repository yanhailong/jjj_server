package com.jjg.game.slots.game.hotfootball.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.hotfootball.HotFootballConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HOT_FOOTBALL_TYPE, cmd = HotFootballConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("response pool value")
public class ResHotFootballPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResHotFootballPoolValue(int code) {
        super(code);
    }
}
