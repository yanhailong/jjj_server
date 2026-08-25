package com.jjg.game.activity.adsReward.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("视频福利奖励档位")
public class AdsRewardDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("解锁该档位需要的观看次数")
    public int requiredCount;
}
