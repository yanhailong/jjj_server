package com.jjg.game.poker.game.blackjack.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/5 20:24
 */
@ProtobufMessage
@ProtoDesc("21点手牌信息")
public class BlackJackCardInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("手牌")
    public List<Integer> cardIds;
    @ProtoDesc("总点数")
    public int totalPoint;
}
