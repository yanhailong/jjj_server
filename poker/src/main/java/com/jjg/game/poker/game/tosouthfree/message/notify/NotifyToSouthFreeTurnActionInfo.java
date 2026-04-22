package com.jjg.game.poker.game.tosouthfree.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;
import com.jjg.game.poker.game.tosouthfree.message.bean.ToSouthFreeActionInfo;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_FREE, cmd = ToSouthFreeConstant.MsgBean.NOTIFY_TURN_ACTION_INFO, resp = true)
@ProtoDesc("响应南方前进-免费玩家回合行动信息")
public class NotifyToSouthFreeTurnActionInfo extends AbstractNotice {
    @ProtoDesc("牌桌操作信息")
    public ToSouthFreeActionInfo actionInfo;
}
