package com.jjg.game.activity.common.message.res;

import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 18:16
 */
@ProtobufMessage
@ProtoDesc("通知活动变化")
public class NotifyActivityChange extends AbstractNotice {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("活动类型")
    public int activityType;
    @ProtoDesc("活动状态  2开启 3结束")
    public int status;
}
