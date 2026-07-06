package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.superGolf.SuperGolfConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SUPER_GOLF_TYPE, cmd = SuperGolfConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("返回奖池值")
public class ResSuperGolfPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResSuperGolfPoolValue(int code) {
        super(code);
    }
}
