package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 通知房间退出的提示
 * @author 2CL
 */
@ProtoDesc("通知房间退出的提示")
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE,
    cmd = MessageConst.CoreMessage.NOTIFY_EXIT,
    resp = true
)
public class NotifyExitRoom extends AbstractNotice {
    @ProtoDesc("多语言ID")
    public int langId;
}
