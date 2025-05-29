package com.vegasnight.game.core.pb;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtoDesc;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/5/26 15:22
 */
@ProtobufMessage(messageType = MessageConst.CertifyMessage.TYPE, cmd = MessageConst.CertifyMessage.RES_LOGIN,resp = true)
@ProtoDesc("登录返回")
public class ResLogin extends AbstractResponse{
    @ProtoDesc("玩家id")
    public long playerId;

    public ResLogin(int code) {
        super(code);
    }
}
