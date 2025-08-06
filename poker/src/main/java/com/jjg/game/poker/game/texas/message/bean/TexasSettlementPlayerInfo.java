package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerSettlementInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 20:02
 */
@ProtobufMessage
@ProtoDesc("德州结算玩家信息")
public class TexasSettlementPlayerInfo {
    @ProtoDesc("基本玩家结算信息")
    public PokerPlayerSettlementInfo pokerPlayerSettlementInfo;
    @ProtoDesc("当前牌")
    public List<Integer> cards;
    @ProtoDesc("牌型")
    public int cardType;
    @ProtoDesc("手牌")
    public List<Integer> handCards;
}
