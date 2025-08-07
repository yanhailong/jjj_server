package com.jjg.game.poker.game.texas.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("德州记录玩家信息")
public class TexasHistoryPlayerInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("玩家名")
    public String playerName;
    @ProtoDesc("操作类型")
    public int operationType;
    @ProtoDesc("德州位置0为庄家")
    public int index;
    @ProtoDesc("押注值")
    public long betValue;
    @ProtoDesc("手牌信息")
    public List<Integer> cardIds;
}
