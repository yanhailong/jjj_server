package com.jjg.game.activity.common.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 11:24
 */
public class BaseActivityDetailInfo {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public int detailId;
    @ProtoDesc("领取状态 1不可领取 2可领取 3已领取 4已购买")
    public int claimStatus = 1;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewardItems;
}
