package com.jjg.game.activity.piggybank.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("储钱罐")
public class PiggyBankDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("当前进度")
    public long progress;
    @ProtoDesc("需要充值金额")
    public int rechargePrice;
    @ProtoDesc("剩余时间")
    public long remainTime;
    @ProtoDesc("是否已经满了")
    public boolean isFull;
    @ProtoDesc("初始金币")
    public long baseValue;
}
