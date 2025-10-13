package com.jjg.game.activity.officialawards.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 官方派奖记录
 *
 * @author lm
 * @date 2025/9/9 17:19
 */
@ProtobufMessage
@ProtoDesc("官方派奖记录")
public class OfficialAwardsShowRecord {
    @ProtoDesc("时间")
    public long recordTime;
    @ProtoDesc("昵称")
    public String name;
    @ProtoDesc("获奖数量")
    public long num;
}
