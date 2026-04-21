package com.jjg.game.poker.game.tosouthfree.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("南方前进推荐牌组")
public class ToSouthFreeRecommendCards {
    @ProtoDesc("推荐牌 ID 列表")
    public List<Integer> cards;

    public ToSouthFreeRecommendCards() {
    }

    public ToSouthFreeRecommendCards(List<Integer> cards) {
        this.cards = cards;
    }
}
