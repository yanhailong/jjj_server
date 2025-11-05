package com.jjg.game.activity.dailylogin.message.bean;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:00
 */
@ProtobufMessage
@ProtoDesc("每日签到详情")
public class DailyLoginDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("类型 1连续 2累计")
    public int type;
    @ProtoDesc("需要天数")
    public int needDays;
}
