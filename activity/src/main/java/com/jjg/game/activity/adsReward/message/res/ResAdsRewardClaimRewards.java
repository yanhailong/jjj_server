package com.jjg.game.activity.adsReward.message.res;

import com.jjg.game.activity.adsReward.message.bean.AdsRewardActivityInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,
        cmd = ActivityConstant.MsgBean.RES_ADS_REWARD_CLAIM, resp = true)
@ProtoDesc("响应领取视频福利档位奖励")
public class ResAdsRewardClaimRewards extends AbstractResponse {
    @ProtoDesc("本次领取的奖励")
    public List<ItemInfo> infoList;
    @ProtoDesc("更新后的视频福利活动信息")
    public AdsRewardActivityInfo activityInfo;

    public ResAdsRewardClaimRewards(int code) {
        super(code);
    }
}
