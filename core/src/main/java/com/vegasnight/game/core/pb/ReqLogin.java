package com.vegasnight.game.core.pb;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtoDesc;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2025/5/26 11:14
 */
@ProtobufMessage(messageType = MessageConst.CertifyMessage.TYPE, cmd = MessageConst.CertifyMessage.REQ_LOGIN)
@ProtoDesc("登录请求")
public class ReqLogin extends AbstractMessage{
    @ProtoDesc("验证token")
    public String token;
}
