package com.jjg.game.slots.game.wolfmoon.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("消除后补齐图标信息")
public class WolfMoonCascade {
    @ProtoDesc("本轮中奖图标信息")
    public WolfMoonIconInfo rewardIconInfo;
    @ProtoDesc("补齐图标")
    public List<KVInfo> addIconInfos;
}
