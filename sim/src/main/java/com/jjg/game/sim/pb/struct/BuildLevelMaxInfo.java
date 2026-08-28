package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("建筑等级上限信息")
public class BuildLevelMaxInfo {
    @ProtoDesc("建筑id")
    public int buildingId;
    @ProtoDesc("旧上限")
    public int oldMax;
    @ProtoDesc("新上限")
    public int newMax;
}
