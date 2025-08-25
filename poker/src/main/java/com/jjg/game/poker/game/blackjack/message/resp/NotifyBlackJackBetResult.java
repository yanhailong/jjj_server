package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;

/**
 * @author lm
 * @date 2025/8/5 19:20
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_BET_RESULT, resp = true)
@ProtoDesc("21点下注结果 普通下注和ACE")
public class NotifyBlackJackBetResult extends AbstractNotice {
    @ProtoDesc("类型")
    public int type;
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("下注金额")
    public long betValue;
    @ProtoDesc("总下注金额")
    public long totalBetValue;
}
