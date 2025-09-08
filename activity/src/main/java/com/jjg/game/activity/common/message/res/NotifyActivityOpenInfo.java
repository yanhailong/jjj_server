package com.jjg.game.activity.common.message.res;

import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/5 17:10
 */
@ProtobufMessage
@ProtoDesc("通知玩家开放的活动类型")
public class NotifyActivityOpenInfo extends AbstractNotice {
    @ProtoDesc("开放的活动类型")
    public List<ActivityInfo> activityTypes;
}
