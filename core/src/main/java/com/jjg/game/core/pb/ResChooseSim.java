package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE, cmd = MessageConst.CoreMessage.RES_CHOOSE_SIM,resp = true)
@ProtoDesc("选择模拟经营游戏")
public class ResChooseSim extends AbstractResponse {

    public ResChooseSim(int code) {
        super(code);
    }
}
