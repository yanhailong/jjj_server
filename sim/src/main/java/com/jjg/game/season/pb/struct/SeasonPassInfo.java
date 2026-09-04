package com.jjg.game.season.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("通行证状态")
public class SeasonPassInfo {
    @ProtoDesc("PassList.id")
    public int passId;
    public List<SeasonPassLevelInfo> levels;
}
