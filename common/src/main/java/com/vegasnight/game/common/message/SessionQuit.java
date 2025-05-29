package com.vegasnight.game.common.message;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.NOTIFY_SESSION_QUIT, toPbFile = false)
public class SessionQuit {
    public String sessionId;
}
