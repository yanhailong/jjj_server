package com.jjg.game.poker.game.blackjack.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 19:47
 */

@ProtobufMessage
@ProtoDesc("21点玩家信息")
public class BlackJackPlayerInfo {
    @ProtoDesc("玩家基本信息")
    public PokerPlayerInfo pokerPlayerInfo;
    @ProtoDesc("手牌信息")
    public List<BlackJackCardInfo> cardInfos;
    @ProtoDesc("当前牌索引")
    public int currentCardIds;
}
