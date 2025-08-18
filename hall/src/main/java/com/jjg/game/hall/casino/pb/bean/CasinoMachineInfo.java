package com.jjg.game.hall.casino.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pb.ItemInfo;

/**
 * @author lm
 * @date 2025/8/18 14:55
 */
@ProtobufMessage
@ProtoDesc("机台信息")
public class CasinoMachineInfo {
    @ProtoDesc("机台id")
    public long machineId;
    @ProtoDesc("机台状态")
    public int state;
    @ProtoDesc("BuildingFunction配置表id")
    public int configId;
    @ProtoDesc("雇员配置表id")
    public int employmentId;
    @ProtoDesc("雇员结束时间")
    public long employmentEndTime;
    @ProtoDesc("机台建造升级结束时间")
    public long buildLvUpEndTime;
    @ProtoDesc("收益开始时间")
    public long profitStartTime;
    @ProtoDesc("加速花费道具")
    public ItemInfo itemInfo;
}
