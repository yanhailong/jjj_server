package com.jjg.game.activity.common.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 11:21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_ACTIVITY_INFO_BY_TYPE)
@ProtoDesc("请求活动信息通过活动类型")
public class ReqActivityInfoByType extends AbstractMessage {
    @ProtoDesc("活动类型")
    public int activityType;
}

