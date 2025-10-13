package com.jjg.game.activity.growthfund.message.res;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.growthfund.message.bean.GrowthFundDetailInfo;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 17:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.RES_GROWTH_FUND_DETAIL_INFO, resp = true)
@ProtoDesc("响应成长基金活动详细信息")
public class ResGrowthFundDetailInfo extends AbstractResponse {
    @ProtoDesc("活动详细信息")
    public List<GrowthFundDetailInfo> detailInfo;

    public ResGrowthFundDetailInfo(int code) {
        super(code);
    }
}
