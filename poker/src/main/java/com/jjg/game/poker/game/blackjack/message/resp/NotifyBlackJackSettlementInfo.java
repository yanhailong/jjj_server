package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 19:36
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE,cmd = BlackJackConstant.MsgBean.NOTIFY_BLACK_JACK_SETTLEMENT_INFO,resp = true)
@ProtoDesc("通知21点结算信息")
public class NotifyBlackJackSettlementInfo extends AbstractNotice {
    @ProtoDesc("玩家基本信息")
    public List<PokerPlayerSettlementInfo> settlementInfos;
    @ProtoDesc("结束时间")
    public long endTime;
    @ProtoDesc("庄家牌信息")
    public List<Integer> cardIds;
}
