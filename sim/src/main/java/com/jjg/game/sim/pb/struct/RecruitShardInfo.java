package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/22
 */
@ProtobufMessage
@ProtoDesc("招募后的碎片信息")
public class RecruitShardInfo {
    public int id;
    public int count;
    @ProtoDesc("获得的碎片")
    public List<ItemInfo> items;
}
