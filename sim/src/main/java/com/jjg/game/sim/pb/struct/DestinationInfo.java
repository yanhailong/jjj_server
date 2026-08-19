package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/27
 */
@ProtobufMessage
@ProtoDesc("目的地信息")
public class DestinationInfo {
    @ProtoDesc("序列id")
    public int index;
    @ProtoDesc("建筑")
    public int buildingId;
    @ProtoDesc("设备")
    public int deviceId;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewards;
    @ProtoDesc("购买游客该目的地奖励是否已领取 (重连时据此判断剩余可领)")
    public boolean claimed;
    @ProtoDesc("交互时间(单位:毫秒)")
    public int interactTime;
}
