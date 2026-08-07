package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("多人任务人数信息")
public class CoopMemberInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("任务配置id")
    public int taskId;
    @ProtoDesc("当前人数")
    public int memberCount;
}
