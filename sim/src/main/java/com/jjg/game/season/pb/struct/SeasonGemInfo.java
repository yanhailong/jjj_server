package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("赛季宝石信息")
public class SeasonGemInfo {
    @ProtoDesc("宝石配置ID")
    public int configId;
    @ProtoDesc("背包道具ID")
    public int itemId;
    @ProtoDesc("形状类型")
    public int type;
    @ProtoDesc("流派")
    public int genre;
    @ProtoDesc("品质")
    public int rarity;
    @ProtoDesc("配置效果值")
    public int buff;
    @ProtoDesc("背包数量")
    public long count;
    @ProtoDesc("已镶嵌数量")
    public int equippedCount;
    @ProtoDesc("名称多语言ID")
    public int nameLanguageId;
    @ProtoDesc("描述多语言ID")
    public int descLanguageId;
    @ProtoDesc("客户端资源名")
    public String icon;
}
