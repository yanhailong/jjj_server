package com.jjg.game.activity.cashcow.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 10:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CASH_COW_TOTAL_POOL,resp = true)
@ProtoDesc("响应摇钱树总奖池")
public class ResCashCowTotalPool extends AbstractResponse {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("总奖池数")
    public long totalNum;

    public ResCashCowTotalPool(int code) {
        super(code);
    }
}
