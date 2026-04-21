package com.jjg.game.poker.game.tosouthblood.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthBloodConstant.MsgBean.REQ_GO_READY)
@ProtoDesc("南方前进-血战玩家请求进行准备")
public class ReqToSouthBloodGoReady extends AbstractMessage {
    @ProtoDesc("1准备 2取消")
    public int status;
}
