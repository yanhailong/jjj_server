package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 离线收益结算
 *
 * @author 11
 * @date 2026/5/29
 */
@ProtobufMessage
@ProtoDesc("离线收益结算")
public class OfflineReward {
    @ProtoDesc("1倍可领取收益")
    public List<ItemInfo> rewards;
    @ProtoDesc("有效结算时长(分)")
    public int offlineMinutes;
    @ProtoDesc("离线上限时长(分)")
    public int capMinutes;
    @ProtoDesc("广告收益倍数")
    public String adMultiplier;
}
