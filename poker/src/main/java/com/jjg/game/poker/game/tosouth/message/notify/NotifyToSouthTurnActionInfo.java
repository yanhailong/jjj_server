package com.jjg.game.poker.game.tosouth.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

import com.jjg.game.poker.game.tosouth.message.bean.ToSouthActionInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.NOTIFY_TURN_ACTION_INFO, resp = true)
@ProtoDesc("响应南方前进玩家回合行动信息")
public class NotifyToSouthTurnActionInfo extends AbstractNotice {
    @ProtoDesc("牌桌操作信息")
    public ToSouthActionInfo actionInfo;
}
