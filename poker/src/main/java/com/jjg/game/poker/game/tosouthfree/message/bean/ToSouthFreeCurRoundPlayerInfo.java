package com.jjg.game.poker.game.tosouthfree.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("南方前进当前轮玩家公开信息")
public class ToSouthFreeCurRoundPlayerInfo {
    public long playerId;
    @ProtoDesc("座位 ID")
    public int seatId;
    @ProtoDesc("本轮中是否已pass，pass不能再出牌")
    public boolean passed;
    @ProtoDesc("本轮中牌的剩余数量")
    public int cardCount;
}
