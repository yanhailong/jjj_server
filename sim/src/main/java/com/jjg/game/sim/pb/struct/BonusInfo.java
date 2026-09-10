package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("建筑加成信息")
public class BonusInfo {
    @ProtoDesc("BuildingAreaTable表的typeValue值")
    public int typeValue;
    @ProtoDesc("加成信息 key为Code.BUILD_BONUS_*，value为该来源的实际增加值，不含基础值")
    public List<KVInfo> bonus;
}
