package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbsNodeMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/9/25 11:35
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.REQ_ACTIVITY_INFOS,
        toPbFile = false, resp = true)
@ProtoDesc("请求活动信息")
public class ReqActivityInfos extends AbsNodeMessage {
}
