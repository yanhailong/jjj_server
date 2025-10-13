package com.jjg.game.activity.growthfund.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 11:28
 */
@ProtobufMessage
@ProtoDesc("成长基金活动信息")
public class GrowthFundActivityInfo {
    @ProtoDesc("活动详细信息")
    public List<GrowthFundDetailInfo> detailInfos;
    @ProtoDesc("当前等级")
    public long currentLevel;
    @ProtoDesc("售价")
    public int sellingPrice;
    @ProtoDesc("原价")
    public int originalPrice;
    @ProtoDesc("总计获得")
    public long totalGet;
}
