package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.MessageTypeDef.SESSION_TYPE, cmd = MessageConst.SessionConst.NOTIFY_SWITCH_NODE, toPbFile = false)
public class SwitchNodeMessage extends AbstractMessage {

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
