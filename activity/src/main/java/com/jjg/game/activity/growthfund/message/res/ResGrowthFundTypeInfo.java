package com.jjg.game.activity.growthfund.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.growthfund.message.bean.GrowthFundActivityInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY,cmd = ActivityConstant.MsgBean.RES_GROWTH_FUND_TYPE_INFO,resp = true)
@ProtoDesc("响应成长基金活动类型信息")
public class ResGrowthFundTypeInfo extends AbstractResponse {
    @ProtoDesc("活动信息")
    public List<GrowthFundActivityInfo> activityData;

    public ResGrowthFundTypeInfo(int code) {
        super(code);
    }
}
