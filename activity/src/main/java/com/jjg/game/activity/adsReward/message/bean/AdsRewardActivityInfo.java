package com.jjg.game.activity.adsReward.message.bean;

import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("视频福利活动信息")
public class AdsRewardActivityInfo {
    @ProtoDesc("活动基础信息")
    public ActivityInfo activityInfo;
    @ProtoDesc("活动开始时间")
    public long startTime;
    @ProtoDesc("活动结束时间")
    public long endTime;
    @ProtoDesc("距离每日重置的剩余时间")
    public long resetRemainTime;
    @ProtoDesc("今日有效观看次数")
    public int watchCount;
    @ProtoDesc("今日观看次数上限")
    public int dailyLimit;
    @ProtoDesc("今日是否已经达到观看上限")
    public boolean completed;
    @ProtoDesc("奖励档位")
    public List<AdsRewardDetailInfo> detailInfos;
}
