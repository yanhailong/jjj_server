package com.jjg.game.ploy.games.airraid.pb.cluster;

import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/5/12
 */
@ProtobufMessage(toPbFile = false)
public class PlayerBetInfo {
    //玩家ID
    public long playerId;
    //头像ID
    public int headImgId;
    //下注金额
    public long betAmount;
    //注单索引(0或1)
    public int betIndex;
}
