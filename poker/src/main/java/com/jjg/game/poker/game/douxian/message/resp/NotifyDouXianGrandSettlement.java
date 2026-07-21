package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;
import com.jjg.game.poker.game.douxian.message.bean.DouXianGrandSettlementPlayerInfo;

import java.util.List;

/**
 * 第4回合结束(或所有对手认输)后的大结算通知，DESIGN.md 8.12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE, cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_GRAND_SETTLEMENT, resp = true)
@ProtoDesc("斗仙牌通知大结算")
public class NotifyDouXianGrandSettlement extends AbstractNotice {
    @ProtoDesc("各玩家最终输赢明细")
    public List<DouXianGrandSettlementPlayerInfo> playerResults;
}
