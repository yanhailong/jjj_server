package com.jjg.game.season.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("通行证奖励")
public class SeasonPassRewardsInfo {
    @ProtoDesc("道具")
    public List<ItemInfo> items;
    @ProtoDesc("0.未解锁  1.已解锁  2.已领取")
    public int status;
    @ProtoDesc("配置价格")
    public String price;
    @ProtoDesc("渠道商品id")
    public String channelProductId;
}
