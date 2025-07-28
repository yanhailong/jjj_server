package com.jjg.game.table.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 玩家金币变化
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("玩家金币更新")
public class PlayerChangedGold {

    @ProtoDesc("玩家ID")
    public long playerId;

    @ProtoDesc("玩家场上赢的金币")
    public long playerWinGold;

    @ProtoDesc("玩家场上下注金币")
    public long playerBetGold;
}
