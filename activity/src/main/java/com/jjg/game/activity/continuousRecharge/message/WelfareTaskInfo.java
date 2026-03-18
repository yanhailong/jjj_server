package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 累计福利任务详情
 * @author 11
 * @date 2026/3/5
 */
@ProtobufMessage
@ProtoDesc("累计福利任务详情")
public class WelfareTaskInfo {
    @ProtoDesc("配置ID")
    public int cfgId;
    @ProtoDesc("目标值")
    public long target;
    @ProtoDesc("奖励道具列表")
    public List<ItemInfo> rewardItems;
    @ProtoDesc("领取状态 1不可领取 2可领取 3已领取")
    public int claimStatus;
}
