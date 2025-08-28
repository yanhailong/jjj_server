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
 * @date 2025/7/29 10:04
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.BLACK_JACK_TYPE, cmd = BlackJackConstant.MsgBean.NOTIFY_CUT_CARD, resp = true)
@ProtoDesc("通知玩家分牌结果")
public class NotifyBlackJackCutCard extends AbstractNotice {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("玩家牌信息")
    public List<BlackJackCardInfo> cardInfoList;
    @ProtoDesc("自动牌id")
    public int autoCard;
    @ProtoDesc("当前牌索引")
    public int currentCardIds;
    @ProtoDesc("结束时间")
    public long overTime;
    @ProtoDesc("下一个操作人")
    public long operationId;
}
