package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import java.util.Map;

@ProtoDesc("赛季商店商品")
public class SeasonShopItemInfo {
    @ProtoDesc("商品配置ID")
    public int id;
    @ProtoDesc("显示顺序")
    public int order;
    @ProtoDesc("获得道具")
    public Map<Integer, Long> goods;
    @ProtoDesc("购买消耗")
    public Map<Integer, Long> cost;
    @ProtoDesc("是否每日重置限购")
    public boolean resetDaily;
    @ProtoDesc("限购次数，负数表示不限")
    public int purchaseLimit;
    @ProtoDesc("当前周期已购买次数")
    public int purchased;
    @ProtoDesc("名称多语言ID")
    public int languageId;
    @ProtoDesc("客户端资源名")
    public String icon;
}
