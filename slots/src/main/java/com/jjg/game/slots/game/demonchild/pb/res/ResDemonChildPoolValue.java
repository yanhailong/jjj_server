package com.jjg.game.slots.game.demonchild.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.demonchild.constant.DemonChildConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CAPTAIN_JACK, cmd = DemonChildConstant.MsgBean.RES_DEMON_CHILD_POOL_VALUE, resp = true)
@ProtoDesc("返回奖池")
public class ResDemonChildPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResDemonChildPoolValue(int code) {
        super(code);
    }
}
