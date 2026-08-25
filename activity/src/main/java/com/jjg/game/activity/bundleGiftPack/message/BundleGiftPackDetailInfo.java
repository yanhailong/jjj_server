package com.jjg.game.activity.bundleGiftPack.message;

import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("集合礼包活动数据详情")
public class BundleGiftPackDetailInfo extends BaseActivityDetailInfo {
    @ProtoDesc("礼包数据")
    public List<BundleGiftPackInfo> packInfoList;
    @ProtoDesc("集合礼包价格")
    public String buyPrice;
    @ProtoDesc("集合礼包渠道商品id")
    public String channelProductId;
    @ProtoDesc("结束时间")
    public long endTime;
}
