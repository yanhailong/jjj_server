package com.jjg.game.core.pb.activity;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.base.gameevent.ActivityChangeEvent;

/**
 * @author lm
 * @date 2025/9/23 19:46
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTIFY_ACTIVITY_SERVER_CHANGE, resp = true)
@ProtoDesc("通知活动变化")
public class NotifyActivityServerChange extends AbstractNotice {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("操作类型 1.更新活动 2.更新活动详细 3.删除活动")
    public ActivityChangeEvent.ChangeType operationType;
}


