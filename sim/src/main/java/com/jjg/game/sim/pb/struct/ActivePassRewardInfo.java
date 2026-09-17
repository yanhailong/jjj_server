package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("活跃通行证奖励领取状态")
public class ActivePassRewardInfo {
    public int rewardId;
    /** 位标记：1免费、2初级、4高级；奖励内容和累计积分门槛由 PassReward 配置读取。 */
    public int claimedTracks;
}
