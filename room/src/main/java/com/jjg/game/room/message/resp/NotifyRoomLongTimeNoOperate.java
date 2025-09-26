package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 通知房间长时间未操作提示
 *
 * @author 2CL
 */
@ProtoDesc("通知房间长时间未操作的提示")
@ProtobufMessage(
        messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
        cmd = RoomMessageConstant.RespMsgBean.NOTIFY_ROOM_LONG_TIME_NO_OPERATE,
        resp = true
)
public class NotifyRoomLongTimeNoOperate extends AbstractNotice {

    @ProtoDesc("多语言ID")
    public int langId;
}
