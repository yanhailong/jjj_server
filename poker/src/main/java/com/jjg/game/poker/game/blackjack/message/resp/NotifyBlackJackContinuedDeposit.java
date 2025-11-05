package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/11/5 13:34
 */
@ProtoDesc("21点请求续押")
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_BLACKJACK_CONTINUED_DEPOSIT, resp = true)
public class NotifyBlackJackContinuedDeposit extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("下注金额")
    public List<Long> betValueList;
    @ProtoDesc("总下注金额")
    public long totalBetValue;
}
