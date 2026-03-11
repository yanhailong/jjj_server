package com.jjg.game.activity.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/8 10:03
 */
@ProtobufMessage
@ProtoDesc("活动信息")
public class ActivityInfo {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("活动类型")
    public int activityType;
    @ProtoDesc("活动状态 0活动开关关闭 1未开启 2开启 3结束")
    public int status;
}
