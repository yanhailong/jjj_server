package com.jjg.game.activity.dailylogin.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("每日签到活动信息")
public class DailyLoginActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<DailyLoginDetailInfo> detailInfos;
    @ProtoDesc("累计天数")
    public long cumulativeDay;
    @ProtoDesc("剩余时间")
    public long remainTime;
}
