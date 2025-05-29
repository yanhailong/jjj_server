package com.vegasnight.game.common.pb;

import com.vegasnight.game.common.constant.MessageConst;
import com.vegasnight.game.common.proto.ProtoDesc;
import com.vegasnight.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2022/5/17
 */
@ProtobufMessage(messageType = MessageConst.ToClientConst.TYPE, cmd = MessageConst.ToClientConst.RES_HEART_BEAT,resp = true)
@ProtoDesc("心跳返回")
public class ResHeartBeat {
    public long time;

    public ResHeartBeat(long time) {
        this.time = time;
    }
}
