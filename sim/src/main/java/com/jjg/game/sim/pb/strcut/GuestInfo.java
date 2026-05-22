package com.jjg.game.sim.pb.strcut;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

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
    @ProtoDesc("剩余未完成的目的地序列 (顺序固定)")
    public List<KVInfo> destinations;
}
