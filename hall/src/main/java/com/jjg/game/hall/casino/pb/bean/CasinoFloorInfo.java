package com.jjg.game.hall.casino.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.pb.struct.ItemInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/8/18 14:55
 */
@ProtobufMessage
@ProtoDesc("楼层信息")
public class CasinoFloorInfo {
    @ProtoDesc("楼层id")
    public int floorId;
    @ProtoDesc("楼层状态")
    public int state;
    @ProtoDesc("机台信息")
    public List<CasinoMachineInfo> casinoMachineInfos;
    @ProtoDesc("打扫结束时间")
    public long cleaningEndTime;
    @ProtoDesc("加速花费道具")
    public ItemInfo itemInfo;
}
