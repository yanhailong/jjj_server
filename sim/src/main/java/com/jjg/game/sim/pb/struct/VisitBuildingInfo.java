package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("拜访可见建筑信息")
public class VisitBuildingInfo {
    @ProtoDesc("建筑配置id")
    public int id;
    @ProtoDesc("建筑等级")
    public int level;
}
