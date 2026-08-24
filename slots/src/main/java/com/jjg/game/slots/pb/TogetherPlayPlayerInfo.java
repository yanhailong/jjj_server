package com.jjg.game.slots.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("好友同玩玩家信息")
public class TogetherPlayPlayerInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("头像框id")
    public int headFrame;
    @ProtoDesc("本次进入净输赢金币")
    public long winGold;
}
