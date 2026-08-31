package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("在线收益信息")
public class OnlineRewardInfo {
    @ProtoDesc("每小时收益，与OfflineReward.rewards一致，itemId = 1.金币 2.能量 3.服务能力 4.游戏上限 5.知名度 6.曝光度 12.场景经验")
    public List<ItemInfo> hourlyRewards;
    @ProtoDesc("视频今日已使用次数")
    public int adUsedCount;
    @ProtoDesc("视频每日次数上限")
    public int adDailyLimit;
    @ProtoDesc("视频每次可领取的收益时长，小时")
    public int adRewardHours;
    @ProtoDesc("视频冷却结束时间，毫秒时间戳，0表示无冷却")
    public long adCdEndTime;
    @ProtoDesc("钻石今日已使用次数")
    public int diamondUsedCount;
    @ProtoDesc("钻石每日次数上限")
    public int diamondDailyLimit;
    @ProtoDesc("钻石每次可领取的收益时长，小时")
    public int diamondRewardHours;
    @ProtoDesc("下一次钻石领取消耗；次数用完时为空")
    public ItemInfo diamondCost;
}
