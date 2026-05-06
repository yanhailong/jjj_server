package com.jjg.game.poker.game.tosouthfree.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("南方前进-免费玩家信息")
public class ToSouthFreePlayerInfo {
    @ProtoDesc("基本玩家信息")
    public PokerPlayerInfo pokerPlayerInfo;
    @ProtoDesc("剩余手牌数量")
    public int handCardCount;
    @ProtoDesc("高亮手牌列表 (仅对自己可见，包含2、炸弹、连对)")
    public List<Integer> highlightCards;
}
