package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/8/25 17:35
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_ALL_KICK_OUT,resp = true,toPbFile = false)
@ProtoDesc("gm请求刷新服务器状态")
public class ReqAllKickout {
    public int langId;
}
