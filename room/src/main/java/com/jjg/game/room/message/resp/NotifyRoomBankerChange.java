package com.jjg.game.room.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.room.message.RoomMessageConstant;

/**
 * 通知房间庄家变化
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.ROOM_TYPE,
    cmd = RoomMessageConstant.RespMsgBean.NOTIFY_ROOM_BANKER_CHANGE,
    resp = true
)
@ProtoDesc("通知房间庄家变化")
public class NotifyRoomBankerChange extends AbstractNotice {

    @ProtoDesc("玩家")
    public long playerId;

    @ProtoDesc("操作 1，上庄 2 下庄")
    public int operate;
}
