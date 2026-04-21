package com.jjg.game.poker.game.tosouthfree.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthFreeConstant.MsgBean.NOTIFY_PLAYER_READY, resp = true)
@ProtoDesc("通知南方前进-免费玩家准备")
public class NotifyToSouthFreePlayerReady extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("1准备 2取消")
    public int status;
}
