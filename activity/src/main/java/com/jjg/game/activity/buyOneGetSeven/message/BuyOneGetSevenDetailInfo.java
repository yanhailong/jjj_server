package com.jjg.game.activity.buyOneGetSeven.message;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("买一送七活动数据详情")
public class BuyOneGetSevenDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("奖励解锁时间戳，毫秒；0表示前置奖励尚未领取")
    public long reciveEndTime;
}
