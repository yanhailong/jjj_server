package com.jjg.game.common.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.CLUSTER_CONNECT_REGISTER, toPbFile = false)
public class ClusterRegsiterMsg {
    public String nodePath;
}
