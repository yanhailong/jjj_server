package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/5/12
 */
@ProtobufMessage
@ProtoDesc("兑现信息")
public class AirRaidCashOutInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("兑现倍率(万分比)")
    public int cashOutMultiplier;
    @ProtoDesc("赢取金额")
    public long winAmount;
    @ProtoDesc("注单索引")
    public int betIndex;
}
