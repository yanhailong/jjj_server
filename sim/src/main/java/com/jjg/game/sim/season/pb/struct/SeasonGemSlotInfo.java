package com.jjg.game.sim.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;

@ProtoDesc("赛季宝石槽位")
public class SeasonGemSlotInfo {
    @ProtoDesc("槽位序号")
    public int slot;
    @ProtoDesc("已镶嵌宝石道具ID，0表示空")
    public int itemId;
}
