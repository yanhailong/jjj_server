package com.vegasnight.game.common.pb;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtoDesc;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2022/5/17
 */
@ProtobufMessage(messageType = MessageConst.ToClientConst.TYPE, cmd = MessageConst.ToClientConst.REQ_HEART_BEAT)
@ProtoDesc("心跳请求")
public class ReqHeartBeat {
    public boolean none;
}
