package com.jjg.game.activity.firstpayment.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.firstpayment.message.bean.FirstPaymentActivityInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_FIRST_PAYMENT_TYPE_INFO,resp = true)
@ProtoDesc("首充活动类型信息")
public class ResFirstPaymentTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<FirstPaymentActivityInfo> activityData;

    public ResFirstPaymentTypeInfo(int code) {
        super(code);
    }
}
