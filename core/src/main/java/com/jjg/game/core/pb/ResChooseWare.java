package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractResponse;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_CHOOSE_WARE,resp = true)
@ProtoDesc("选择游戏场次进入")
public class ResChooseWare extends AbstractResponse {

    public ResChooseWare(int code) {
        super(code);
    }
}
