package com.jjg.game.slots.game.goldsnakefortune.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("金钱信息")
public class GoldSnakeFortuneCoinInfo {
    @ProtoDesc("坐标")
    public int index;
    @ProtoDesc("金额")
    public long value;
}
