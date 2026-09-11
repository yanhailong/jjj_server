package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("挖矿礼包")
public class MiningBundleInfo {
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
    @ProtoDesc("礼包类型，取MiningBundleShop.BundleType：1免费 2广告 3消耗道具购买")
    public int mode;
    @ProtoDesc("礼包内容")
    public List<ItemInfo> goods;
    @ProtoDesc("兼容字段：消耗数量字符串；币种及数量请使用cost")
    public String price;
    @ProtoDesc("礼包名称多语言ID")
    public int nameLanguageId;
    @ProtoDesc("广告冷却结束时间，毫秒时间戳，0表示当前无冷却")
    public long adCdEndTime;
    @ProtoDesc("单次购买消耗的道具ID及数量，免费/广告为空")
    public List<ItemInfo> cost;
}
