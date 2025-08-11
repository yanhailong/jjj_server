package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;

/**
 * @author lm
 * @date 2025/8/11 09:50
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_BLACKJACK_DOUBLE_BET_INFO, resp = true)
@ProtoDesc("21点双倍押注")
public class NotifyBlackJackDoubleBetInfo extends AbstractNotice {
    @ProtoDesc("拿牌信息")
    public NotifyBlackJackPutCard putCardInfo;
    @ProtoDesc("下注金额")
    public long betValue;
}
