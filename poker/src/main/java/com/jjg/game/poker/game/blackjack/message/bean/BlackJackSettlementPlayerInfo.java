package com.jjg.game.poker.game.blackjack.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 19:47
 */

@ProtobufMessage
@ProtoDesc("21点结算玩家信息")
public class BlackJackSettlementPlayerInfo {
    @ProtoDesc("玩家基本信息")
    public PokerPlayerSettlementInfo baseInfo;
    @ProtoDesc("手牌信息")
    public List<BlackJackCardInfo> cardInfos;
}
