package com.jjg.game.hall.pb.friendroom.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 请求好友房账单历史
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.REQ_FRIEND_ROOM_BILL_HISTORY
)
@ProtoDesc("请求好友房账单历史")
public class ReqFriendRoomBillHistory {

    @ProtoDesc("房间ID")
    public long roomId;
}
