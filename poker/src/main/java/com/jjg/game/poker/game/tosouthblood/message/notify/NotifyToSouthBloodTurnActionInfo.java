package com.jjg.game.poker.game.tosouthblood.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;
import com.jjg.game.poker.game.tosouthblood.message.bean.ToSouthBloodActionInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthBloodConstant.MsgBean.NOTIFY_TURN_ACTION_INFO, resp = true)
@ProtoDesc("响应南方前进玩家回合行动信息")
public class NotifyToSouthBloodTurnActionInfo extends AbstractNotice {
    @ProtoDesc("牌桌操作信息")
    public ToSouthBloodActionInfo actionInfo;
}
