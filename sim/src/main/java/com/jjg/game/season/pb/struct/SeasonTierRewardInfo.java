package com.jjg.game.season.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("段位奖励条目")
public class SeasonTierRewardInfo {
    @ProtoDesc("段位配置ID")
    public int tierId;
    @ProtoDesc("段位多语言ID")
    public int rankLanguageId;
    @ProtoDesc("奖励物品列表")
    public List<ItemInfo> rewards;
    @ProtoDesc("是否已获得")
    public boolean obtained;
    @ProtoDesc("该段位获得的徽章 (勋章id, 0表示无)")
    public int seasonBadge;
}
