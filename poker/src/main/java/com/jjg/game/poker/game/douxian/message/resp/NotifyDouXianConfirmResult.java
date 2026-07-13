package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

/**
 * 玩家确认出牌通知，DESIGN.md 8.8 游戏等待
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_CONFIRM_RESULT, resp = true)
@ProtoDesc("斗仙牌通知确认出牌结果")
public class NotifyDouXianConfirmResult extends AbstractNotice {
    @ProtoDesc("确认出牌的玩家id")
    public long playerId;
    @ProtoDesc("是否所有玩家都已确认(true时客户端应进入结算等待)")
    public boolean allConfirmed;
}
