package com.vegasnight.game.common.message;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.NOTIFY_SWITCH_NODE, toPbFile = false)
public class SwitchNodeMessage {

    public String sessionId;
    //目标节点路径
    public String targetNodePath;

    public long playerId;

    public SwitchNodeMessage(String sessionId, String targetNodePath, long playerId) {
        this.sessionId = sessionId;
        this.targetNodePath = targetNodePath;
        this.playerId = playerId;
    }
}
