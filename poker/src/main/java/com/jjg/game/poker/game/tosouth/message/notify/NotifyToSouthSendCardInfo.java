package com.jjg.game.poker.game.tosouth.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouth.constant.ToSouthConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthConstant.MsgBean.NOTIFY_SEND_CARD_INFO)
@ProtoDesc("南方前进通知发牌信息")
public class NotifyToSouthSendCardInfo extends AbstractNotice {
}
