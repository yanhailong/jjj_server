package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("一次消除下落信息")
public class BountyDuelCascade {
    @ProtoDesc("本次消除中奖图标信息")
    public BountyDuelIconInfo rewardIconInfo;
    @ProtoDesc("补充图标信息，key=位置，value=图标ID")
    public List<KVInfo> addIconInfos;
}
