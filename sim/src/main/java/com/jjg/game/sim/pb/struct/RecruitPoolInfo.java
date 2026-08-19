package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("招募卡池")
public class RecruitPoolInfo {
    public int id;
    @ProtoDesc("时间戳")
    public int endTime;
    @ProtoDesc("多语言id")
    public int langId;
}
