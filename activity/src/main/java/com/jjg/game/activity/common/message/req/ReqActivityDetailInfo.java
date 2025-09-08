package com.jjg.game.activity.common.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/3 11:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_ACTIVITY_DETAIL_INFO)
@ProtoDesc("请求活动详细信息")
public class ReqActivityDetailInfo extends AbstractMessage {
    @ProtoDesc("活动id")
    public long activityId;
    @ProtoDesc("详情id")
    public int detailId;
}
