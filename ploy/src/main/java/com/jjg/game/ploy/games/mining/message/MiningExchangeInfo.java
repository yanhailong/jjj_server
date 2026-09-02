package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("挖矿兑换商品")
public class MiningExchangeInfo {
    @ProtoDesc("配置是否支持发货；false时不可购买")
    public boolean available;
    @ProtoDesc("配置商品不可购买原因")
    public String disabledReason;
    @ProtoDesc("配置ID")
    public int id;
    @ProtoDesc("展示顺序")
    public int order;
    @ProtoDesc("今日购买数量")
    public int boughtToday;
    @ProtoDesc("剩余可购数量，-1不限购")
    public int remaining;
    @ProtoDesc("商品")
    public List<ItemInfo> goods;
    @ProtoDesc("单份道具价格")
    public List<ItemInfo> cost;
}
