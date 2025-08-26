package com.jjg.game.poker.game.blackjack.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
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
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("操作类型")
    public int type;
    @ProtoDesc("玩家的牌信息 发牌结算为所有玩家牌信息 分牌时为玩家自己的牌信息")
    public List<BlackJackCardInfo> cardIdList;
    @ProtoDesc("庄家的牌信息")
    public int cardId;
    @ProtoDesc("自动牌id")
    public int autoCard;
    @ProtoDesc("拿牌id")
    public int sendCardId;
    @ProtoDesc("拿牌后总点数")
    public List<Integer> putCardTotal;
    @ProtoDesc("下注金额")
    public long betValue;
    @ProtoDesc("当前牌组索引")
    public int currentCardIds;
    @ProtoDesc("结算信息")
    public NotifyBlackJackSettlementInfo settlementInfo;
}
