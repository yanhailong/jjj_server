package com.jjg.game.common.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/26 10:05
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_CLIENT_TYPE, cmd = MessageConst.ToClientConst.NOTIFY_KICK_OUT,resp = true)
@ProtoDesc("通知踢人")
public class NotifyKickout extends AbstractNotice{
    @ProtoDesc("多语言ID")
    public int langId;
}
