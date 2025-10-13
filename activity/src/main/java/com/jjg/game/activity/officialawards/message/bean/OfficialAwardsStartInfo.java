package com.jjg.game.activity.officialawards.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/10/11 13:52
 */
@ProtobufMessage
@ProtoDesc("官方派奖开启信息")
public class OfficialAwardsStartInfo {
    @ProtoDesc("开始时间")
    public long startTime;
    @ProtoDesc("几号")
    public int number;
}
