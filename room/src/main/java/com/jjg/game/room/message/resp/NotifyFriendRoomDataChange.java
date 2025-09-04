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
public class NotifyFriendRoomDataChange extends AbstractNotice {

    @ProtoDesc("场上的最新的庄家ID，如果场上没有庄家此值为0")
    public long bankerPlayerId;

    @ProtoDesc("房主准备金")
    public long roomCreatorPredicateCostGold;

    @ProtoDesc("庄家准备金")
    public long bankerPredicateCostGold;
}
