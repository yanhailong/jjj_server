package com.jjg.game.activity.adsReward.message.res;

import com.jjg.game.activity.adsReward.message.bean.AdsRewardActivityInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,
        cmd = ActivityConstant.MsgBean.RES_ADS_REWARD_WATCH, resp = true)
@ProtoDesc("响应视频福利观看完成")
public class ResAdsRewardWatch extends AbstractResponse {
    @ProtoDesc("更新后的视频福利活动信息")
    public AdsRewardActivityInfo activityInfo;

    public ResAdsRewardWatch(int code) {
        super(code);
    }
}
