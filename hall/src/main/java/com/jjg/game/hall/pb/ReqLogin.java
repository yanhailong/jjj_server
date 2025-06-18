package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;

/**
 * @author 11
 * @date 2025/5/26 11:14
 */
@ProtobufMessage(messageType = MessageConst.CertifyMessage.TYPE, cmd = MessageConst.CertifyMessage.REQ_LOGIN)
@ProtoDesc("登录请求")
public class ReqLogin extends AbstractMessage {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("验证token")
    public String token;
}
