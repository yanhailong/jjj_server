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
    @ProtoDesc("建筑")
    public int buildingId;
    @ProtoDesc("设备")
    public int deviceId;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewards;
    @ProtoDesc("是否为有奖励交互点 (购买游客领奖时按此结算, false 则奖励为空)")
    public boolean rewarded;
    @ProtoDesc("购买游客该目的地奖励是否已领取 (重连时据此判断剩余可领)")
    public boolean claimed;
}
