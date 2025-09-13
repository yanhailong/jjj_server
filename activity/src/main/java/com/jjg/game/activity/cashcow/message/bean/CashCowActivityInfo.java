package com.jjg.game.activity.cashcow.message.bean;

import com.jjg.game.common.pb.ItemInfo;
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
    @ProtoDesc("结束时间")
    public long endTime;
    @ProtoDesc("期数")
    public long round;
    @ProtoDesc("重置剩余时间")
    public long resetRemainTime;
    @ProtoDesc("免费道具列表")
    public ItemInfo freeItemInfo;
    @ProtoDesc("免费道具领取状态 true已经领取")
    public boolean freeStatus;
}
