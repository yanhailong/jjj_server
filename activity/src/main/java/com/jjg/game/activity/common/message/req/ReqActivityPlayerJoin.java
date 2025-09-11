package com.jjg.game.activity.common.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/10 16:37
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_ACTIVITY_PLAYER_JOIN)
@ProtoDesc("请求参加活动")
public class ReqActivityPlayerJoin {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public int detailId;
}
