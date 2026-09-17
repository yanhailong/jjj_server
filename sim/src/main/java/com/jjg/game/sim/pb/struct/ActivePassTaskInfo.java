package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("活跃通行证任务")
public class ActivePassTaskInfo {
    public int taskId;
    public int group;
    public int taskType;
    public long progress;
    public long target;
    /** 0进行中，1待领取，2已领取。 */
    public int status;
}
