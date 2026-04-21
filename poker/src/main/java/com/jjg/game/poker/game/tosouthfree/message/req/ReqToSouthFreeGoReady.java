package com.jjg.game.poker.game.tosouthfree.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthFreeConstant.MsgBean.REQ_GO_READY)
@ProtoDesc("南方前进-免费玩家请求进行准备")
public class ReqToSouthFreeGoReady extends AbstractMessage {
    @ProtoDesc("1准备 2取消")
    public int status;
}
