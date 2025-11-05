package com.jjg.game.poker.game.blackjack.message.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;

import java.util.List;

/**
 * @author lm
 * @date 2025/11/5 13:34
 */
@ProtoDesc("21点请求续押")
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.REQ_BLACKJACK_CONTINUED_DEPOSIT)
public class ReqBlackJackContinuedDeposit extends AbstractMessage {
    @ProtoDesc("下注金额")
    public List<Long> betValueList;
}
