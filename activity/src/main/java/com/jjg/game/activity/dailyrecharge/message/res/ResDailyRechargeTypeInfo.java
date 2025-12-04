package com.jjg.game.activity.dailyrecharge.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.dailyrecharge.message.bean.DailyRechargeActivityInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_DAILY_RECHARGE_TYPE_INFO,resp = true)
@ProtoDesc("响应每日充值活动类型信息")
public class ResDailyRechargeTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<DailyRechargeActivityInfo> activityData;

    public ResDailyRechargeTypeInfo(int code) {
        super(code);
    }
}
