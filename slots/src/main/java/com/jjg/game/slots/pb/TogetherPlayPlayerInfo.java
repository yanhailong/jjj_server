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
    @ProtoDesc("该玩家本次为我贡献的提成金币")
    public long commissionGold;
    @ProtoDesc("该玩家当前是否在我的提成名额内")
    public boolean commissionEnabled;
}
