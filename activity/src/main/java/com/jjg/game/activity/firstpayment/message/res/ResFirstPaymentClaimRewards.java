package com.jjg.game.activity.firstpayment.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.firstpayment.message.bean.FirstPaymentDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/4 13:43
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_FIRST_PAYMENT_CLAIM_REWARDS,resp = true)
@ProtoDesc("首充领取活动奖励")
public class ResFirstPaymentClaimRewards extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public long detailId;
    @ProtoDesc("领取奖励信息")
    public List<ItemInfo> infoList;
    @ProtoDesc("详细信息")
    public FirstPaymentDetailInfo detailInfo;

    public ResFirstPaymentClaimRewards(int code) {
        super(code);
    }
}
