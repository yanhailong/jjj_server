package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/5/12
 */
@ProtobufMessage(toPbFile = false)
public class PlayerCashOut {
    //玩家ID
    public long playerId;
    //兑现倍率(万分比)
    public int cashOutMultiplier;
    //赢得金额
    public long winAmount;
    //注单索引(0或1)
    public int betIndex;
}
