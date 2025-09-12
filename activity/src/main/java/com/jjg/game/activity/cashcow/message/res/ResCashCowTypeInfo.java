package com.jjg.game.activity.cashcow.message.res;

import com.jjg.game.activity.cashcow.message.bean.CashCowActivityInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_CASH_COW_TYPE_INFO,resp = true)
@ProtoDesc("响应摇钱树活动类型信息")
public class ResCashCowTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<CashCowActivityInfo> activityData;

    public ResCashCowTypeInfo(int code) {
        super(code);
    }
}
