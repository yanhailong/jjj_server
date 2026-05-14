package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/5/13
 */
@ProtobufMessage
@ProtoDesc("排行榜条目")
public class AirRaidRankInfo {
    @ProtoDesc("玩家信息")
    public AirRaidPlayerInfo playerInfo;
    @ProtoDesc("时间戳(ms)")
    public long time;
    @ProtoDesc("回合坠毁倍数(万分比)")
    public int crashMultiplier;
    @ProtoDesc("回合id")
    public int roundId;
}
