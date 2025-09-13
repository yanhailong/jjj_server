package com.jjg.game.activity.cashcow.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/13 13:34
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_CASH_COW_FREE_REWARDS)
@ProtoDesc("请求领取摇钱树免费奖励")
public class ReqCashCowFreeRewards {
    @ProtoDesc("活动id")
    public long activityId;
}
