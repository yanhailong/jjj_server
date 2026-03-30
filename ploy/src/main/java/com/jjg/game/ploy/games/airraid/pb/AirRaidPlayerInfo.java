package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage
@ProtoDesc("玩家信息")
public class AirRaidPlayerInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像id")
    public long bet;
    @ProtoDesc("是否已兑现")
    public boolean cashedOut;
    @ProtoDesc("兑现倍率")
    public int cashOutMultiplier;
    @ProtoDesc("赢取金额")
    public long winAmount;
}
