package com.jjg.game.poker.game.tosouthfree.message.notify;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.tosouthfree.constant.ToSouthFreeConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SOUTH, cmd = ToSouthFreeConstant.MsgBean.NOTIFY_SEND_CARD_INFO, resp = true)
@ProtoDesc("南方前进-免费通知发牌信息")
public class NotifyToSouthFreeSendCardInfo extends AbstractNotice {
}
