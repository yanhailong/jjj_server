package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/11 16:05
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.REQ_GM, resp = true)
@ProtoDesc("gm请求")
public class ReqGm extends AbstractMessage {
    @ProtoDesc("命令参数")
    public String order;
    @ProtoDesc("玩家id")
    public long playerId;
}
