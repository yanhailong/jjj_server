package com.jjg.game.activity.growthfund.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("成长基金详情")
public class GrowthFundDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("类型 1免费 2付费")
    public int type;
    @ProtoDesc("需要等级")
    public int needLevel;
}
