package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/10/20 11:13
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_GENERATE_ORDER,resp = true)
@ProtoDesc("预下单返回")
public class ResGenerateOrder extends AbstractResponse {
    public String orderId;

    public ResGenerateOrder(int code) {
        super(code);
    }
}
