package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackSettlementPlayerInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 19:36
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE,cmd = BlackJackConstant.MsgBean.NOTIFY_BLACK_JACK_SETTLEMENT_INFO)
@ProtoDesc("通知21点结算信息")
public class NotifyBlackJackSettlementInfo {
    @ProtoDesc("结算玩家信息")
    public List<BlackJackSettlementPlayerInfo> playerSettlementInfos;
}
