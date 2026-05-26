package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/25
 */
@ProtobufMessage
@ProtoDesc("技能信息")
public class GameSkills {
    @ProtoDesc("游戏")
    public int gameType;
    @ProtoDesc("技能信息  propId->level")
    public List<KVInfo> skillInfos;
    @ProtoDesc("解锁的下注额")
    public List<Long> stake;
}
