package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.MessageTypeDef.SESSION_TYPE, cmd = MessageConst.SessionConst.NOTIFY_SESSION_LOGOUT, toPbFile = false)
public class SessionLogout extends AbstractMessage {
    public long playerId;
    public String sessionId;
}
