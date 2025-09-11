package com.jjg.game.core.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 2CL
 */
@ProtobufMessage
public enum ESceneType {
    @ProtoDesc("房间")
    ROOM,
    @ProtoDesc("大厅")
    HALL;
}
