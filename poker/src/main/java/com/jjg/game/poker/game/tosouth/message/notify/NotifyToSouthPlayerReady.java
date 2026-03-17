package com.jjg.game.poker.game.tosouth.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.NOTIFY_PLAYER_READY, resp = true)
@ProtoDesc("通知南方前进玩家准备")
public class NotifyToSouthPlayerReady extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
}
