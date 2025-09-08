package com.jjg.game.activity.common.message.bean;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 11:24
 */
@ProtobufMessage
@ProtoDesc("基础活动详细信息")
public class BaseActivityDetailInfo {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public int detailId;
    @ProtoDesc("领取状态 1不可领取 2可领取 3已领取")
    public int claimStatus;
    @ProtoDesc("奖励")
    public List<ItemInfo> rewardItems;
}
