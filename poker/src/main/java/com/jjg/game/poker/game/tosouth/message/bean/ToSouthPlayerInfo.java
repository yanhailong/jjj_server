package com.jjg.game.poker.game.tosouth.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("南方前进玩家信息")
public class ToSouthPlayerInfo {
    @ProtoDesc("基本玩家信息")
    public PokerPlayerInfo pokerPlayerInfo;
    @ProtoDesc("剩余手牌数量")
    public int handCardCount;
}
