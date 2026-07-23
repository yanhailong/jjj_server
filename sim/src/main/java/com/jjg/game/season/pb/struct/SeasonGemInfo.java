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
    @ProtoDesc("背包数量")
    public long count;
    @ProtoDesc("属性")
    public int buff;
}
