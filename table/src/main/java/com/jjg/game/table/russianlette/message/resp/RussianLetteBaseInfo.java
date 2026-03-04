package com.jjg.game.table.russianlette.message.resp;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.constant.EGamePhase;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘基础信息")
public class RussianLetteBaseInfo {

    @ProtoDesc("游戏阶段信息")
    public EGamePhase eGamePhase;

    @ProtoDesc("房间ID,房间唯一ID")
    public long roomId;

    @ProtoDesc("场次ID")
    public int wareId;

    @ProtoDesc("阶段总时间")
    public int phaseTotalTime;

    @ProtoDesc("阶段结束时间戳")
    public long phaseEndTimestamp;

    @ProtoDesc("服务器当前时间")
    public long serverCurrentTime;
}
