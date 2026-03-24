package com.jjg.game.core.pb.function;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.constant.Code;

/**
 * @author lm
 * @date 2026/3/24 10:47
 */

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_CHECK_FUNCTION_STATE, resp = true)
@ProtoDesc("检测功能开放响应")
public class ResCheckFunctionState extends AbstractResponse {

    public ResCheckFunctionState() {
        super(Code.SUCCESS);
    }
}
