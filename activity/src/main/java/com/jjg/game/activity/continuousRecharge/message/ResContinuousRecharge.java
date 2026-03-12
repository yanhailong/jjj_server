package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/5
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_CONTINUOUS_RECHARGE, resp = true)
@ProtoDesc("响应连续充值活动数据")
public class ResContinuousRecharge extends AbstractResponse {
    @ProtoDesc("活动列表信息")
    public List<ContinuousRechargeDetailInfo> activityData;

    public ResContinuousRecharge(int code) {
        super(code);
    }
}
