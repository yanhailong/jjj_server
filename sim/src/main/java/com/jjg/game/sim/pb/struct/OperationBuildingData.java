package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 数据看板单栋已解锁建筑的细分数据。
 */
@ProtobufMessage
@ProtoDesc("数据看板单栋建筑数据")
public class OperationBuildingData {
    @ProtoDesc("建筑ID")
    public int buildingId;
    @ProtoDesc("建筑类型，对应BuildingAreaTable.type")
    public int buildingType;
    @ProtoDesc("当前等级")
    public int level;
    @ProtoDesc("等级上限")
    public int maxLevel;
    @ProtoDesc("娱乐城当前等级配置的预估建筑等级，未配置为0")
    public int expectedLevel;
    @ProtoDesc("是否显示收益过低提示")
    public boolean incomeTooLow;
    @ProtoDesc("当前交互人数")
    public int currentCapacity;
    @ProtoDesc("当前等级容纳上限")
    public int capacity;
    @ProtoDesc("金币产出/小时")
    public long goldOutputPerHour;
    @ProtoDesc("经验产出/小时")
    public long expOutputPerHour;
    @ProtoDesc("体力产出/小时")
    public long powerOutputPerHour;
    @ProtoDesc("接待区累计交互次数，非接待区为0")
    public int interactionCount;
    @ProtoDesc("曝光度")
    public long exposure;
    @ProtoDesc("知名度")
    public long awareness;
    @ProtoDesc("满意度，万分比")
    public int satisfactionRate;
    @ProtoDesc("运营能力，万分比")
    public int operationRate;
    @ProtoDesc("高级游客品质概率，仅营销部建筑返回")
    public List<OperationVisitorQualityRate> premiumVisitorRates;
    @ProtoDesc("收益过低标签多语言ID，0表示不显示标签")
    public int warningLanguageId;
    @ProtoDesc("当前交互人数是否超过爆红阈值")
    public boolean capacityOverloaded;
}
