package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.RES_NOTIFY_SESSION_VERIFYPASS, toPbFile = false)
public class ResSessionVerifyPass {
    public String sessionId;
    public long playerId;
    public long create;
    public String ip;
    public boolean success;
}
