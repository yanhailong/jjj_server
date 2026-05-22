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
    @ProtoDesc("星级")
    public int star;
    @ProtoDesc("经验")
    public int exp;
    @ProtoDesc("等级")
    public int level;
    @ProtoDesc("剩余未完成的目的地序列 (顺序固定)")
    public List<KVInfo> destinations;
    @ProtoDesc("当前所在建筑id (0=建筑外)")
    public int currentBuildingId;
}
