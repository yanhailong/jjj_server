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
 * @date 2025/8/6 14:34
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_SEND_CARD_INFO, resp = true)
@ProtoDesc("21点通知发牌信息")
public class NotifyBlackJackSendCardInfo extends AbstractNotice {
    @ProtoDesc("超时时间")
    public long overTime;
    @ProtoDesc("所有玩家的牌信息")
    public List<BlackJackCardInfo> cardIdList;
    @ProtoDesc("庄家的牌信息")
    public int cardId;
    @ProtoDesc("下一个操作人id")
    public long operationId;

}
