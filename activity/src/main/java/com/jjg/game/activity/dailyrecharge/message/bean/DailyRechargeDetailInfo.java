package com.jjg.game.activity.dailyrecharge.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("每日充值详情")
public class DailyRechargeDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("类型 1礼包 2累计奖励信息")
    public int type;
    @ProtoDesc("额外参数 礼包时为购买价格 累计奖励时为需要达到的进度")
    public String extraParameters;
    @ProtoDesc("最大限购次数")
    public int maxBuyCount;
    @ProtoDesc("当前购买次数")
    public int currentBuyCount;
    @ProtoDesc("标签")
    public int tag;
}
