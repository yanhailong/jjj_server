package com.jjg.game.poker.game.tosouth.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.REQ_GO_READY)
@ProtoDesc("南方前进玩家请求进行准备")
public class ReqToSouthGoReady extends AbstractMessage {
}
