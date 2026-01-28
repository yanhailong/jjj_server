package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("南方前进玩家结算信息")
public class ToSouthPlayerSettlementInfo {
    @ProtoDesc("玩家 ID")
    public long playerId;
    @ProtoDesc("赢分 (净输赢)")
    public long winScore;
    @ProtoDesc("当前分数 (结算后)")
    public long currentScore;
    @ProtoDesc("剩余手牌")
    public List<Integer> handCards;
    @ProtoDesc("是否赢家")
    public boolean isWinner;
}
