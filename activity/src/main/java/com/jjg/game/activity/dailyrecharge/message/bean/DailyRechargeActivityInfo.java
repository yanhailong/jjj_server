package com.jjg.game.activity.dailyrecharge.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("每日充值活动信息")
public class DailyRechargeActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<DailyRechargeDetailInfo> detailInfos;
    @ProtoDesc("剩余时间")
    public long remainTime;
    @ProtoDesc("当前充值")
    public String currentRecharge;
}
