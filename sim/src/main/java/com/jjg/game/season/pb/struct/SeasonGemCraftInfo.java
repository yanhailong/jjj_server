package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("宝石合成信息")
public class SeasonGemCraftInfo {
    @ProtoDesc("品质")
    public int quality;
    @ProtoDesc("成功率，百分比")
    public int prop;
    @ProtoDesc("消耗的赛季币")
    public int cost;
}
