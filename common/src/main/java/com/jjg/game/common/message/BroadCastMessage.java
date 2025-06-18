package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.BROADCAST_MSG, toPbFile = false)
public class BroadCastMessage {

    public Object msg;

    public BroadCastMessage() {
    }

    public BroadCastMessage(Object msg) {
        this.msg = msg;
    }
}
