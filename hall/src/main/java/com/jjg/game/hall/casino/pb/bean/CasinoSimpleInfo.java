package com.jjg.game.hall.casino.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/8/19 18:17
 */
@ProtobufMessage
@ProtoDesc("机台基本信息")
public class CasinoSimpleInfo {
    @ProtoDesc("机台id")
    public long machineId;
    @ProtoDesc("配置表id")
    public int configId;
    @ProtoDesc("收益开始时间")
    public long profitStartTime;
    @ProtoDesc("收益最大时间")
    public long profitMaxTime;
}
