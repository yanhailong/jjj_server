package com.jjg.game.slots.game.dracula.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.dracula.DraculaConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DRACULA_TYPE, cmd = DraculaConstant.MsgBean.REQ_POOL_INFO)
@ProtoDesc("request pool value")
public class ReqDraculaPoolValue extends AbstractMessage {
    @ProtoDesc("stake value")
    public long stakeVlue;
}
