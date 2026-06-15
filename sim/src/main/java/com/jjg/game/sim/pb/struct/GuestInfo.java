package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/21
 */
@ProtobufMessage
@ProtoDesc("游客信息")
public class GuestInfo {
    @ProtoDesc("游客id")
    public int id;
    @ProtoDesc("唯一id，有值表示这是一次性的")
    public long uid;
    @ProtoDesc("剩余未完成的目的地序列")
    public List<DestinationInfo> destinations;
}
