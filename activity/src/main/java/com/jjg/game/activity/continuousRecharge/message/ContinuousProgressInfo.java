package com.jjg.game.activity.continuousRecharge.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/3/5
 */
@ProtobufMessage
@ProtoDesc("连续充值每日进度信息")
public class ContinuousProgressInfo {
    @ProtoDesc("目标值")
    public long target;
    @ProtoDesc("返利比例")
    public int rebate;
}
