package com.jjg.game.slots.game.dracula.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.dracula.DraculaConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DRACULA_TYPE, cmd = DraculaConstant.MsgBean.RES_POOL_INFO, resp = true)
@ProtoDesc("response pool value")
public class ResDraculaPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResDraculaPoolValue(int code) {
        super(code);
    }
}
