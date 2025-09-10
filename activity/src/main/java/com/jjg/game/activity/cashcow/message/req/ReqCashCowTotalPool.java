package com.jjg.game.activity.cashcow.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 10:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.REQ_CASH_COW_TOTAL_POOL)
@ProtoDesc("请求摇钱树总奖池")
public class ReqCashCowTotalPool {
    @ProtoDesc("活动id")
    public long activityId;
}
