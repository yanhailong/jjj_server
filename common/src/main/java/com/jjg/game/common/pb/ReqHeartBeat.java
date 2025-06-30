package com.jjg.game.common.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2022/5/17
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_CLIENT_TYPE, cmd = MessageConst.ToClientConst.REQ_HEART_BEAT)
@ProtoDesc("心跳请求")
public class ReqHeartBeat {
    public boolean none;
}
