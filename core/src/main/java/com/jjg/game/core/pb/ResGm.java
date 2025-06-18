package com.jjg.game.core.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/6/11 16:05
 */
@ProtobufMessage(messageType = MessageConst.CoreMessage.TYPE, cmd = MessageConst.CoreMessage.RES_GM,resp = true)
@ProtoDesc("gm返回")
public class ResGm extends AbstractResponse{

    public String result;

    public ResGm(int code) {
        super(code);
    }
}
