package com.jjg.game.common.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_CLIENT_TYPE, cmd = MessageConst.ToClientConst.NOTICE_SERVER_STATUS,resp = true)
@ProtoDesc("通知网络状态")
public class NoticeServerStatus {
    @ProtoDesc("状态码")
    public int code = 200;
    public NetStatEnum result;

    public NoticeServerStatus(NetStatEnum result) {
        this.result = result;
    }
}
