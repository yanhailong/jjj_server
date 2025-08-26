package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;

/**
 * @author lm
 * @date 2025/8/11 13:46
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.REPS_BLACKJACK_BUY_ACE, resp = true)
@ProtoDesc("21点购买ACE响应")
public class NotifyBlackJackBuyACE extends AbstractNotice {
    @ProtoDesc("下一个玩家id")
    public long nextPlayerId;
    @ProtoDesc("超时时间戳")
    public long overTime;
    @ProtoDesc("结果true 21点 false 非21点")
    public boolean result;
}
