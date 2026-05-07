package com.jjg.game.poker.game.tosouthblood.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_BLOOD, cmd = ToSouthBloodConstant.MsgBean.NOTIFY_PLAYER_READY, resp = true)
@ProtoDesc("通知南方前进-血战玩家准备")
public class NotifyToSouthBloodPlayerReady extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("1准备 2取消")
    public int status;
}
