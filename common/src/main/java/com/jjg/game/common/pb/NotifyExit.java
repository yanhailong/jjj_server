package com.jjg.game.common.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 通知踢人提示
 * @author 2CL
 */
@ProtoDesc("通知踢人提示")
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    cmd = MessageConst.CoreMessage.NOTIFY_EXIT,
    resp = true
)
public class NotifyExit {
    @ProtoDesc("状态码")
    public int code = 200;
    @ProtoDesc("多语言ID")
    public int langId;
}
