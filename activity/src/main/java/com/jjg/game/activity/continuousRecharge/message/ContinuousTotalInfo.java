package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/3/6
 */
@ProtobufMessage
@ProtoDesc("连续充值活动额外信息")
public class ContinuousTotalInfo {
    @ProtoDesc("连续充值活动累计充值")
    public String totalValue;
    @ProtoDesc("当前天数索引,从0开始")
    public int currentDayIndex;
    @ProtoDesc("当前累计返利比例,万分比")
    public int currentRebateRate;
    @ProtoDesc("预计价值")
    public String estimatedValue;
    @ProtoDesc("实际奖励(金币)")
    public long actualReward;
    @ProtoDesc("今日充值")
    public String todayValue;
}
