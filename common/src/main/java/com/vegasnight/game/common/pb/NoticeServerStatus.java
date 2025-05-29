package com.vegasnight.game.common.pb;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtoDesc;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @since 1.0
 */
@ProtobufMessage(messageType = MessageConst.ToClientConst.TYPE, cmd = MessageConst.ToClientConst.NOTICE_SERVER_STATUS,resp = true)
@ProtoDesc("通知网络状态")
public class NoticeServerStatus {

    NetStatEnum result;

    public NoticeServerStatus(NetStatEnum result) {
        this.result = result;
    }
}
