package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 单栋建筑的实时容纳数据。
 */
@ProtobufMessage
@ProtoDesc("单栋建筑实时容纳数据")
public class OperationBuildingCapacity {
    @ProtoDesc("建筑ID")
    public int buildingId;
    @ProtoDesc("当前交互人数")
    public int currentCapacity;
    @ProtoDesc("当前等级容纳上限")
    public int capacity;
}
