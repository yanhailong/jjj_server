package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CONTINUOUS_RECHARGE_CLAIM_REWARDS,resp = true)
@ProtoDesc("响应连续充值，福利领取奖励")
public class ResContinuousRechargeClaimRewards extends AbstractResponse {
    @ProtoDesc("奖励详情")
    public List<ItemInfo> rewards;
    @ProtoDesc("活动详情")
    public List<ContinuousRechargeDetailInfo> detailInfos;

    public ResContinuousRechargeClaimRewards(int code) {
        super(code);
    }
}
