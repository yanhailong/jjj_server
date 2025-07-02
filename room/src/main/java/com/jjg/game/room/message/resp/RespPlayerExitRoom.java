package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 通知玩家退出房间
 *
 * @author 2CL
 */
@ProtobufMessage(
    resp = true, messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.NOTIFY_EXIT_ROOM
)
public class RespPlayerExitRoom extends AbstractMessage {

    @ProtoDesc("错误码")
    public int code;

    @ProtoDesc("玩家ID")
    public long playerId;
}
