package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

@ProtobufMessage
@ProtoDesc("拜访礼物")
public class VisitGiftInfo {
    @ProtoDesc("礼物id")
    public int id;
    @ProtoDesc("钻石价格")
    public long diamondCost;
    @ProtoDesc("房主获得人气")
    public int popularity;
}
