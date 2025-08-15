package com.jjg.game.hall.friendroom.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("游戏账单信息")
public class GameBillInfo {

    @ProtoDesc("房间ID")
    public long roomId;

    @ProtoDesc("游戏配置ID")
    public int gameCfgId;

    @ProtoDesc("总流水")
    public long totalFlow;

    @ProtoDesc("可以领取的收益")
    public long canTakeIncome;

    @ProtoDesc("总局数")
    public int totalRound;

    @ProtoDesc("总收益")
    public int totalIncome;
}
