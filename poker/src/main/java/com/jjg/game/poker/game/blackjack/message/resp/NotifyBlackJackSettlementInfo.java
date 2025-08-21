package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.blackjack.message.bean.BlackJackSettlementInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 19:36
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_BLACK_JACK_SETTLEMENT_INFO, resp = true)
@ProtoDesc("通知21点结算信息")
public class NotifyBlackJackSettlementInfo extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("操作类型")
    public int type;
    @ProtoDesc("玩家基本信息")
    public List<BlackJackSettlementInfo> settlementInfos;
    @ProtoDesc("庄家牌信息")
    public List<Integer> cardIds;
    @ProtoDesc("显示庄家的牌")
    public boolean showDealer;
    @ProtoDesc("庄家总点数")
    public int totalPoint;
    @ProtoDesc("结束时间")
    public long endTime;
}
