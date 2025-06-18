package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.net.NetAddress;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.NOTIFY_SESSION_ENTER, toPbFile = false)
public class SessionCreate {
    public String sessionId;
    public NetAddress netAddress;
    public long playerId;
    public String nodePath;
    public byte[] loginData;
}
