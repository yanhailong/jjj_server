package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

@ProtobufMessage
@ProtoDesc("神秘符号倍率信息")
public class SuperGolfAddTimesInfo {
    @ProtoDesc("状态 0.正常 1.免费")
    public int status;
    @ProtoDesc("神秘符号个数 -> 倍率列表（与德古拉协议结构对齐，神秘符号机制不需要可留空）")
    public List<KVInfo> times;
}
