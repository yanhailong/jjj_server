package com.jjg.game.poker.game.tosouthblood.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("南方前进-血战当前轮玩家公开信息")
public class ToSouthBloodCurRoundPlayerInfo {
    public long playerId;
    @ProtoDesc("座位 ID")
    public int seatId;
    @ProtoDesc("本轮中是否已pass，pass不能再出牌")
    public boolean passed;
    @ProtoDesc("本轮中牌的剩余数量")
    public int cardCount;
    @ProtoDesc("是否已出完所有手牌（血战：出完后继续打）")
    public boolean isOver;
    @ProtoDesc("出完牌的名次（1:第一名 2:第二名 3:第三名），未出完时为0")
    public int finishRank;
}
