package com.jjg.game.activity.common.message.res;

import com.jjg.game.activity.common.message.bean.ActivityInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/3 18:16
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.NOTIFY_ACTIVITY_CHANGE, resp = true)
@ProtoDesc("通知活动变化")
public class NotifyActivityChange extends AbstractNotice {
    @ProtoDesc("活动信息")
    public List<ActivityInfo> activityInfos;
}
