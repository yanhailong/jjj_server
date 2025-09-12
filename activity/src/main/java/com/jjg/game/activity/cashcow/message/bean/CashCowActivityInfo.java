package com.jjg.game.activity.cashcow.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("摇钱树类型活动信息")
public class CashCowActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<CashCowDetailInfo> detailInfos;
    @ProtoDesc("当前进度")
    public long currentProgress;
    @ProtoDesc("剩余时间")
    public long endTime;
    @ProtoDesc("期数")
    public long round;
}
