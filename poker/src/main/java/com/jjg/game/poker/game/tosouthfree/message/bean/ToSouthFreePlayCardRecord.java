package com.jjg.game.poker.game.tosouthfree.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("南方前进玩家出牌记录")
public class ToSouthFreePlayCardRecord {
    @ProtoDesc("座位 ID")
    public int seatId;
    @ProtoDesc("打出的牌组")
    public List<Integer> playedCards;
}
