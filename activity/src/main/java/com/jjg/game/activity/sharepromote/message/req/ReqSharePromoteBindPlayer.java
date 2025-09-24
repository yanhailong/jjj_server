package com.jjg.game.activity.sharepromote.message.req;

import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/16 15:45
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.ACTIVITY, cmd = ActivityConstant.MsgBean.REQ_SHARE_PROMOTE_BIND_PLAYER)
@ProtoDesc("请求绑定玩家")
public class ReqSharePromoteBindPlayer {
    @ProtoDesc("邀请码")
    public String invitationCode;
}
