package com.jjg.game.activity.bundleGiftPack.message;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("集合礼包数据")
public class BundleGiftPackInfo {
    @ProtoDesc("配置表中的商品id")
    public int id;
    @ProtoDesc("获得的道具")
    public List<ItemInfo> items;
    @ProtoDesc("渠道商品id")
    public String channelProductId;
    @ProtoDesc("购买金额")
    public String buyPrice;
    @ProtoDesc("领取状态 1不可领取 2可领取 3已领取 4已购买")
    public int claimStatus = 1;
}
