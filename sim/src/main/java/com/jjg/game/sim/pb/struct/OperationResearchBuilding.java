package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * Slot游戏研发列表中的单项数据。
 */
@ProtobufMessage
@ProtoDesc("Slot游戏研发项")
public class OperationResearchBuilding {
    @ProtoDesc("建筑ID")
    public int buildingId;
    @ProtoDesc("建筑解锁的游戏ID")
    public int gameType;
    @ProtoDesc("状态：1已解锁，2当前研发项，3后续未解锁")
    public int state;
    @ProtoDesc("是否满足全部条件并可直接研发")
    public boolean canResearch;
    @ProtoDesc("当前研发项尚未满足的条件；已满足条件不返回")
    public List<BuildingTips> unmetConditions;
}
