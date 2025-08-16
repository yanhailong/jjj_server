package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackCardInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/15 17:44
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_BLACKJACK_SPECIAL_SETTLEMENT, resp = true)
@ProtoDesc("通知特殊结算")
public class NotifyBlackJackSpecialSettlement extends AbstractNotice {
    @ProtoDesc("结算信息")
    public NotifyBlackJackSettlementInfo settlementInfo;
    @ProtoDesc("所有玩家的牌信息")
    public List<BlackJackCardInfo> cardIdList;

}
