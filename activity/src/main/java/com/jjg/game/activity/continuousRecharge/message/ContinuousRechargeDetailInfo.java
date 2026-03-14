package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/6
 */
@ProtobufMessage
@ProtoDesc("响应连续充值活动数据详情")
public class ContinuousRechargeDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("参与的活动  0.未参与  1.七日连充  2.累计福利")
    public int join;
    @ProtoDesc("当前活动阶段  0.未开启  1.七日连充  2.累计福利")
    public int phase;
    @ProtoDesc("每天总的返利比例,万分比")
    public List<KVInfo> dailyAllRebateList;
    @ProtoDesc("连续充值活动的信息")
    public List<DailyContinuousInfo> dailyContinuousInfosList;
    @ProtoDesc("连续充值活动额外信息")
    public ContinuousTotalInfo continuousTotalInfo;
    @ProtoDesc("累计福利活动信息")
    public WelfareInfo welfareInfo;
    @ProtoDesc("开始时间")
    public long startTime;
    @ProtoDesc("结束时间")
    public long endTime;
}
