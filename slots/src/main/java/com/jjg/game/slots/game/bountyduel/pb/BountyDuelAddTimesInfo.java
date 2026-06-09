package com.jjg.game.slots.game.bountyduel.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("连续中奖倍数信息")
public class BountyDuelAddTimesInfo {
    @ProtoDesc("模式状态：0 普通，1 免费")
    public int status;
    @ProtoDesc("连续中奖次数对应倍数")
    public List<KVInfo> times;
}
