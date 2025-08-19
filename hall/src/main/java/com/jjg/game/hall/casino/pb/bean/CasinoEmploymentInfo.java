package com.jjg.game.hall.casino.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/8/19 13:58
 */
@ProtobufMessage
@ProtoDesc("雇员信息")
public class CasinoEmploymentInfo {
    @ProtoDesc("雇员配置表id")
    public int employmentId;
    @ProtoDesc("雇员结束时间")
    public long employmentEndTime;
}
