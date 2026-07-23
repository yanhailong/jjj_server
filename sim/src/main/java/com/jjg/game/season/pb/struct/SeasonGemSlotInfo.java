package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("赛季宝石槽位")
public class SeasonGemSlotInfo {
    @ProtoDesc("槽位序号")
    public int slot;
    @ProtoDesc("已镶嵌宝石道具ID，0表示空")
    public int itemId;
    @ProtoDesc("宝石id")
    public int gemId;
    @ProtoDesc("形状类型")
    public int type;
    @ProtoDesc("流派")
    public int genre;
    @ProtoDesc("属性")
    public int buff;
}
