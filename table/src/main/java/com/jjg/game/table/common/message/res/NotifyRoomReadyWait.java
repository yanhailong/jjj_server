package com.jjg.game.table.common.message.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 通知进入房间等待状态
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BET_GENERAL_TYPE,
    resp = true,
    cmd = RoomMessageConstant.RespMsgBean.NOTIFY_ROOM_WAIT_READY
)
@ProtoDesc("通知房间进入")
public class NotifyRoomReadyWait extends AbstractResponse {
    @ProtoDesc("房间ID")
    public long roomId;
    @ProtoDesc("等待时间结束的时间戳")
    public long waitEndTime;

    public NotifyRoomReadyWait(int code) {
        super(code);
    }
}
