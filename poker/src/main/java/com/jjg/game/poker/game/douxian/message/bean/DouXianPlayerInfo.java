package com.jjg.game.poker.game.douxian.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.common.message.bean.PokerPlayerInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("斗仙牌玩家信息")
public class DouXianPlayerInfo {
    @ProtoDesc("玩家基本信息")
    public PokerPlayerInfo pokerPlayerInfo;
    @ProtoDesc("手牌数量(仅自己能收到具体手牌id，其余人只知道数量)")
    public int handCardNum;
    @ProtoDesc("三个区域的摆牌信息")
    public List<DouXianZonePlacementInfo> zones;
    @ProtoDesc("是否已确认出牌")
    public boolean confirmed;
    @ProtoDesc("是否托管中")
    public boolean hosting;
    @ProtoDesc("是否已认输")
    public boolean conceded;
    @ProtoDesc("是否已准备(仅等待阶段WAIT_READY有意义)")
    public boolean ready;
}
