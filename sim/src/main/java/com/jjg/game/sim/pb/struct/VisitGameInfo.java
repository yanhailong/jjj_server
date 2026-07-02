package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("拜访可试玩slot游戏")
public class VisitGameInfo {
    @ProtoDesc("游戏类型")
    public int gameType;
    @ProtoDesc("房主研发属性 propId->等级")
    public List<KVInfo> skills;
}
