package com.jjg.game.core.pb.function;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/3/24 10:47
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.REQ_CHECK_FUNCTION_STATE)
@ProtoDesc("请求检测功能开放")
public class ReqCheckFunctionState {
    @ProtoDesc("功能开放id")
    public int gameFunctionId;
}
