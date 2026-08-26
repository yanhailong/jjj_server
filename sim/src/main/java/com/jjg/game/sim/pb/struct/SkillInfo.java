package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("技能信息")
public class SkillInfo {
    @ProtoDesc("技能propId")
    public int propId;
    @ProtoDesc("技能等级")
    public int level;
    @ProtoDesc("建筑产出加成")
    public int addOutPut;
}
