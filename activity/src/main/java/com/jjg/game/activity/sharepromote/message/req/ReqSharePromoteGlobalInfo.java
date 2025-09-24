package com.jjg.game.activity.sharepromote.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 15:26
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_GLOBAL_INFO)
@ProtoDesc("请求推广分享总览信息")
public class ReqSharePromoteGlobalInfo {
}
