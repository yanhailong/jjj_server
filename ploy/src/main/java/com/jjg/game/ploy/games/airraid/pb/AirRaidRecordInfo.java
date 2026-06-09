package com.jjg.game.ploy.games.airraid.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/5/19
 */
@ProtobufMessage
@ProtoDesc("空袭个人历史记录条目")
public class AirRaidRecordInfo {
    @ProtoDesc("投注金额")
    public long betAmount;
    @ProtoDesc("兑现倍率(万分比)")
    public int cashOutMultiplier;
    @ProtoDesc("赢得金额")
    public long winAmount;
    @ProtoDesc("时间戳(ms)")
    public long timestamp;
}
