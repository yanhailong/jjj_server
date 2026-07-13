package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 托管状态变化通知，DESIGN.md 8.14
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_HOSTING_STATE, resp = true)
@ProtoDesc("斗仙牌通知托管状态")
public class NotifyDouXianHostingState extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("是否托管中")
    public boolean hosting;
}
