package com.jjg.game.poker.game.tosouthblood.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthblood.constant.ToSouthBloodConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH_BLOOD, cmd = ToSouthBloodConstant.MsgBean.NOTIFY_SEND_CARD_INFO, resp = true)
@ProtoDesc("南方前进-血战通知发牌信息")
public class NotifyToSouthBloodSendCardInfo extends AbstractNotice {
}
