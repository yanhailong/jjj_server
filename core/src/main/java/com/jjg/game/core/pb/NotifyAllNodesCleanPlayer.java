package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/4/2
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_CLEAN_PLAYER,resp = true, toPbFile = false)
@ProtoDesc("推送到其他节点清除玩家信息")
public class NotifyAllNodesCleanPlayer extends AbstractNotice {
    public List<Long> playerIds;
}
