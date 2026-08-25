package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("建筑升级条件")
public class CasinoUpgradeCondition {
    @ProtoDesc("建筑id")
    public int buildingId;
    @ProtoDesc("该建筑是否解锁")
    public boolean unlock;
    @ProtoDesc("需要的等级")
    public int level;
    @ProtoDesc("能否升级")
    public boolean canUpgrade;
}
