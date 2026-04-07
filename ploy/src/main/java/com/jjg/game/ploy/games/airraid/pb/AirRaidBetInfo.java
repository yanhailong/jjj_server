package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/3/27
 */
@ProtobufMessage
@ProtoDesc("押注信息")
public class AirRaidBetInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("注单索引")
    public int betIndex;
    @ProtoDesc("下注额")
    public long bet;
    @ProtoDesc("是否已兑现")
    public boolean cashedOut;
    @ProtoDesc("倍数")
    public int times;
    @ProtoDesc("赢奖")
    public long win;
}
