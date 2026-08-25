package com.jjg.game.activity.adsReward.message.res;

import com.jjg.game.activity.adsReward.message.bean.AdsRewardActivityInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,
        cmd = ActivityConstant.MsgBean.RES_ADS_REWARD_INFO, resp = true)
@ProtoDesc("响应视频福利活动信息")
public class ResAdsRewardInfo extends AbstractResponse {
    @ProtoDesc("当前开启的视频福利活动")
    public AdsRewardActivityInfo activityInfo;

    public ResAdsRewardInfo(int code) {
        super(code);
    }
}
