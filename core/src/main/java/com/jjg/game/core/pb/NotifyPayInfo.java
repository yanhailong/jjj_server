package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;


/**
 * @author 11
 * @date 2025/11/13 14:24
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.NOTIFY_PAY_INFO,resp = true)
@ProtoDesc("推送玩家基础信息变化")
public class NotifyPayInfo extends AbstractNotice {
    @ProtoDesc("订单id")
    public String orderId;
    @ProtoDesc("当前累计充值次数")
    public int allRechargeCount;
}
