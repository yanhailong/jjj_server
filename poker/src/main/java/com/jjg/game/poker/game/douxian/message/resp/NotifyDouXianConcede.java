package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 玩家认输通知，DESIGN.md 8.9
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_CONCEDE, resp = true)
@ProtoDesc("斗仙牌通知玩家认输")
public class NotifyDouXianConcede extends AbstractNotice {
    @ProtoDesc("认输玩家id")
    public long playerId;
    @ProtoDesc("房间是否因此直接进入大结算(仅剩一名未认输玩家)")
    public boolean triggerGrandSettlement;
}
