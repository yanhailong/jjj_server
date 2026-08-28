package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 数据看板顶部经营数据总览。
 */
@ProtobufMessage
@ProtoDesc("数据看板顶部经营数据总览")
public class OperationDashboardOverview {
    @ProtoDesc("所有已解锁Slot建筑金币产出/分钟")
    public long goldOutputPerMinute;
    @ProtoDesc("所有已解锁Slot建筑经验产出/分钟")
    public long expOutputPerMinute;
    @ProtoDesc("固定窗口内的当前交互人数")
    public int currentCapacity;
    @ProtoDesc("当前已解锁Slot建筑和休息区的总容纳上限")
    public int totalCapacity;
    @ProtoDesc("获客人数/分钟")
    public long customerAcquisitionPerMinute;
    @ProtoDesc("知名度")
    public long awareness;
    @ProtoDesc("满意度，万分比，10000表示100.00%")
    public int satisfactionRate;
    @ProtoDesc("服务能力")
    public long serviceCapacity;
    @ProtoDesc("运营能力，万分比，10000表示100.00%")
    public int operationRate;
    @ProtoDesc("高级游客品质概率，只返回蓝、紫、橙品质")
    public List<OperationVisitorQualityRate> premiumVisitorRates;
}
