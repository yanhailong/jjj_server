package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("特殊游客信息")
public class SpecialGuestInfo {
    @ProtoDesc("游客生成配置id")
    public int id;
    @ProtoDesc("游客道具id")
    public int itemId;
    @ProtoDesc("单个游客数量")
    public int count;
    @ProtoDesc("付费类型  0.广告  1.钻石  2.美刀")
    public int costType;
    @ProtoDesc("价格")
    public String price;
    @ProtoDesc("当日已经观看广告/购买次数")
    public int dailyBuyCount;
    @ProtoDesc("单日限购次数")
    public int dailyLimitCount;
    @ProtoDesc("广告冷却时间，单位分钟")
    public int viewCd;
    @ProtoDesc("广告冷却结束时间，毫秒时间戳")
    public long viewCdEndTime;
    @ProtoDesc("游客当前等级的固定产出和额外掉落，未解锁时使用1级配置")
    public List<ItemInfo> output;
    @ProtoDesc("礼包出现游客的种类个数")
    public int visitorGiftPackCount;
}
