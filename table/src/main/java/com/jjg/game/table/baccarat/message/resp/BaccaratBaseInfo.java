package com.jjg.game.table.baccarat.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("百家乐基础信息")
public class BaccaratBaseInfo {

    @ProtoDesc("房间ID,房间唯一ID")
    public long roomId;

    @ProtoDesc("场次ID")
    public int wareId;

    @ProtoDesc("剩余牌数量")
    public int remainingCardNum;

    @ProtoDesc("总的牌数量")
    public int totalCardNum;

    @ProtoDesc("游戏阶段信息")
    public EGamePhase eGamePhase;

    @ProtoDesc("阶段总时间")
    public int phaseTotalTime;

    @ProtoDesc("阶段结束时间戳")
    public long phaseEndTimestamp;

    @ProtoDesc("服务器当前时间")
    public long serverCurrentTime;
}
