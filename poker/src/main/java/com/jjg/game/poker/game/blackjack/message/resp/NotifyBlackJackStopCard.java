package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerSampleCardOperation;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/15 17:10
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_BLACKJACK_STOP_CARD, resp = true)
@ProtoDesc("21通知停牌")
public class NotifyBlackJackStopCard extends AbstractNotice {
    @ProtoDesc("操作信息")
    public NotifyPokerSampleCardOperation operation;
    @ProtoDesc("自动牌id")
    public int autoCardId;
    @ProtoDesc("自动牌总点数")
    public List<Integer> autoCardTotal;
}
