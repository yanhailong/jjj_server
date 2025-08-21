package com.jjg.game.poker.game.blackjack.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/18 11:12
 */
@ProtobufMessage
@ProtoDesc("21点结算信息")
public class BlackJackSettlementInfo {
    @ProtoDesc("玩家基本信息")
    public PokerPlayerSettlementInfo settlementInfo;
    @ProtoDesc("牌组结算状态0失败 1获胜 2平局 ")
    public List<Integer> cardGroupState;
}
